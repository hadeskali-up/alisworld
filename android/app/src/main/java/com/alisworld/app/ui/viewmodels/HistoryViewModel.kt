package com.alisworld.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisworld.app.data.ApiClient
import com.alisworld.app.data.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

private const val MalaysiaUtcOffsetHours = 8L

data class HistoryUiState(
    val isLoading: Boolean = false,
    val items: List<HistoryItem> = emptyList(),
    val hasMore: Boolean = false,
    val error: String? = null,
    val selectedSymbol: String? = null
) {
    val symbols: List<String> get() = items.map { it.symbol }.distinct().sorted()
    val filteredItems: List<HistoryItem> get() = selectedSymbol?.let { symbol -> items.filter { it.symbol == symbol } } ?: items
    val availableHistoryPnl: Double get() = items.sumOf { it.profit }
    val listPnl: Double get() = filteredItems.sumOf { it.profit }
    val dailyPnlMalaysia: Double get() {
        val malaysiaToday = LocalDateTime.now().toLocalDate()
        return items.filter { item -> parseDate(item.closeTime) == malaysiaToday }.sumOf { it.profit }
    }

    private fun parseDate(value: String): LocalDate? = runCatching {
        LocalDateTime.parse(value.take(19)).toLocalDate()
    }.getOrNull()
}

class HistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    private val pageSize = 200

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            ApiClient.instance.getHistory(limit = pageSize)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = response.items,
                        hasMore = response.hasMore,
                        error = null,
                        selectedSymbol = _uiState.value.selectedSymbol?.takeIf { selected -> response.items.any { it.symbol == selected } }
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message ?: "Unknown error")
                }
        }
    }

    fun selectSymbol(symbol: String?) {
        _uiState.value = _uiState.value.copy(selectedSymbol = symbol)
    }
}
