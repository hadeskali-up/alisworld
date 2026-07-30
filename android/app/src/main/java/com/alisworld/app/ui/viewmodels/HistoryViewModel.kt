package com.alisworld.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisworld.app.data.ApiClient
import com.alisworld.app.data.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = false,
    val items: List<HistoryItem> = emptyList(),
    val hasMore: Boolean = false,
    val error: String? = null,
    val selectedSymbol: String? = null,
    val symbols: List<String> = emptyList(),
    val dailyPnl: Double = 0.0,
    val allTimePnl: Double = 0.0,
    val filteredListPnl: Double = 0.0
)

class HistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private val pageSize = 100

    fun loadHistory() {
        viewModelScope.launch {
            val selectedSymbol = _uiState.value.selectedSymbol
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            ApiClient.instance.getHistory(limit = pageSize, symbol = selectedSymbol)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = response.items,
                        hasMore = response.hasMore,
                        symbols = response.symbols,
                        dailyPnl = response.dailyPnl,
                        allTimePnl = response.allTimePnl,
                        filteredListPnl = response.filteredListPnl,
                        error = null
                    )
                }
                .onFailure { error -> _uiState.value = _uiState.value.copy(isLoading = false, error = error.message ?: "Unable to load history") }
        }
    }

    fun selectSymbol(symbol: String?) {
        _uiState.value = _uiState.value.copy(selectedSymbol = symbol)
        loadHistory()
    }
}
