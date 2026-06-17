package net.steamcardexchange.dealfinder.data

/**
 * A single Steam Card Exchange listing for a fully available trading card set.
 *
 * @property appId        The Steam application id of the game.
 * @property name         The human readable game name.
 * @property cardsInSet   How many cards make up a complete set.
 * @property setsAvailable How many complete sets are currently offered.
 * @property totalCredits The summed "You Pay" price (in credits) for one full set.
 * @property url          Direct link to the listing on steamcardexchange.net.
 */
data class GameDeal(
    val appId: String,
    val name: String,
    val cardsInSet: Int,
    val setsAvailable: Int,
    val totalCredits: Int,
    val url: String
)
