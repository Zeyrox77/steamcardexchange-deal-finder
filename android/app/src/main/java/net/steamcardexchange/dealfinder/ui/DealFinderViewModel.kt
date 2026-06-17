package net.steamcardexchange.dealfinder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.steamcardexchange.dealfinder.data.GameDeal
import net.steamcardexchange.dealfinder.data.SteamCardRepository

/** Immutable snapshot of everything the screen needs to render. */
data class DealFinderUiState(
    val isLoading: Boolean = false,
    val statusMessage: String = "Ready to fetch data.",
    val progress: Float = 0f,
    val deals: List<GameDeal> = emptyList(),
    val errorMessage: String? = null
)

class DealFinderViewModel(
    private val repository: SteamCardRepository = SteamCardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DealFinderUiState())
    val uiState: StateFlow<DealFinderUiState> = _uiState.asStateFlow()

    /** Kicks off the fetch + scrape pipeline. Ignored while already running. */
    fun fetchDeals() {
        if (_uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                isLoading = true,
                statusMessage = "Fetching global list from API...",
                progress = 0f,
                deals = emptyList(),
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val deals = repository.findCheapestDeals(
                    limit = TOP_LIMIT,
                    maxConcurrency = MAX_CONCURRENCY,
                    onProgress = { completed, total ->
                        val fraction = if (total > 0) completed.toFloat() / total else 0f
                        _uiState.update { state ->
                            state.copy(
                                progress = fraction,
                                statusMessage = if (completed == 0) {
                                    "Found $total fully available sets. Checking prices..."
                                } else {
                                    "Checked $completed of $total games..."
                                }
                            )
                        }
                    }
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        progress = 1f,
                        deals = deals,
                        statusMessage = if (deals.isEmpty()) {
                            "No available sets found. Try again later."
                        } else {
                            "Done! Showing the ${deals.size} cheapest sets. Tap a row to open it."
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        progress = 0f,
                        statusMessage = "Something went wrong.",
                        errorMessage = e.message ?: "Unknown error while fetching deals."
                    )
                }
            }
        }
    }

    companion object {
        private const val TOP_LIMIT = 50
        private const val MAX_CONCURRENCY = 20
    }
}
