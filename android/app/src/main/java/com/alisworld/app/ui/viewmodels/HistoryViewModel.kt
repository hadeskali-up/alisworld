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
    val error: String? = null
)

class HistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    private var currentOffset = 0
    private val pageSize = 50
    
    fun loadHistory() {
        currentOffset = 0
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            ApiClient.instance.getHistory(limit = pageSize, offset = 0)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = response.items,
                        hasMore = response.hasMore,
                        error = null
                    )
                    currentOffset = pageSize
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
        }
    }
    
    fun loadMore() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            ApiClient.instance.getHistory(limit = pageSize, offset = currentOffset)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = _uiState.value.items + response.items,
                        hasMore = response.hasMore
                    )
                    currentOffset += pageSize
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load more"
                    )
                }
        }
    }
}
