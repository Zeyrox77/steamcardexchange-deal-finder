package net.steamcardexchange.dealfinder.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Talks to steamcardexchange.net: pulls the full inventory list from the API and then
 * scrapes each candidate's page to read the real per-card prices.
 *
 * This is the Android/Kotlin equivalent of the original Python desktop tool.
 */
class SteamCardRepository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Internal representation of a game before we know the real total price. */
    private data class Candidate(
        val appId: String,
        val name: String,
        val cardsInSet: Int,
        val setsAvailable: Int
    )

    /**
     * Finds the cheapest fully-available card sets.
     *
     * @param limit          maximum number of deals to return (cheapest first).
     * @param maxConcurrency how many game pages to scrape in parallel.
     * @param onProgress     invoked as pages are checked: (completed, total).
     */
    suspend fun findCheapestDeals(
        limit: Int = 50,
        maxConcurrency: Int = 20,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): List<GameDeal> {
        val candidates = fetchCandidates()

        val total = candidates.size
        val completed = AtomicInteger(0)
        // Report the initial total so the UI knows the denominator straight away.
        onProgress(0, total)

        val semaphore = Semaphore(maxConcurrency)

        val deals = coroutineScope {
            candidates.map { candidate ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        val deal = fetchGameDeal(candidate)
                        val done = completed.incrementAndGet()
                        onProgress(done, total)
                        deal
                    }
                }
            }.awaitAll()
        }.filterNotNull()

        return deals
            .sortedBy { it.totalCredits }
            .take(limit)
    }

    /**
     * Calls the inventory API and keeps only games where a complete set is available
     * to buy (i.e. owned-count equals set size and at least one set is in stock).
     */
    private suspend fun fetchCandidates(): List<Candidate> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(API_URL)
            .header("User-Agent", USER_AGENT)
            .build()

        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SteamCardException("API request failed with HTTP ${response.code}")
            }
            response.body?.string() ?: throw SteamCardException("Empty API response")
        }

        parseCandidates(body)
    }

    /**
     * Parses the API JSON. The payload is an array of rows shaped like:
     * `[[appid, name], worth, ?, [cardsInSet, ownedInSet, setsAvailable], ...]`.
     */
    private fun parseCandidates(json: String): List<Candidate> {
        val trimmed = json.trim()
        val rows: JSONArray = if (trimmed.startsWith("{")) {
            org.json.JSONObject(trimmed).optJSONArray("data") ?: JSONArray()
        } else {
            JSONArray(trimmed)
        }

        val result = ArrayList<Candidate>()
        for (i in 0 until rows.length()) {
            try {
                val row = rows.getJSONArray(i)
                val nameInfo = row.getJSONArray(0)
                val appId = nameInfo.get(0).toString()
                val name = nameInfo.getString(1)

                val setData = row.getJSONArray(3)
                val cardsInSet = setData.getString(0).toInt()
                val ownedInSet = setData.getString(1).toInt()
                val setsAvailable = setData.getString(2).toInt()

                // Only keep complete, in-stock sets (matches the original tool's filter).
                if (cardsInSet == ownedInSet && setsAvailable > 0) {
                    result.add(
                        Candidate(
                            appId = appId,
                            name = name,
                            cardsInSet = cardsInSet,
                            setsAvailable = setsAvailable
                        )
                    )
                }
            } catch (_: Exception) {
                // Skip malformed rows, just like the Python version.
                continue
            }
        }
        return result
    }

    /**
     * Downloads a single game's page and sums the real "Price (You Pay)" values for
     * the cards in the set. Returns null on any failure so callers can ignore it.
     */
    private fun fetchGameDeal(candidate: Candidate): GameDeal? {
        val url = gamePageUrl(candidate.appId)
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            val html = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string() ?: return null
            }

            val text = Jsoup.parse(html).text()
            val prices = PRICE_REGEX.findAll(text)
                .map { it.groupValues[1].toInt() }
                .toList()

            if (prices.size < candidate.cardsInSet) return null

            val total = prices.take(candidate.cardsInSet).sum()

            GameDeal(
                appId = candidate.appId,
                name = candidate.name,
                cardsInSet = candidate.cardsInSet,
                setsAvailable = candidate.setsAvailable,
                totalCredits = total,
                url = url
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun gamePageUrl(appId: String): String =
        "https://www.steamcardexchange.net/index.php?inventorygame-appid-$appId"

    companion object {
        private const val API_URL =
            "https://www.steamcardexchange.net/api/request.php?GetInventory"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        // Matches e.g. "Price (You Pay): 12 c"
        private val PRICE_REGEX = Regex("""Price \(You Pay\):\s*(\d+)\s*c""")
    }
}

/** Thrown when the inventory API cannot be reached or returns an error. */
class SteamCardException(message: String) : Exception(message)
