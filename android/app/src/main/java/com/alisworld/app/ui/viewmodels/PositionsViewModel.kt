package com.alisworld.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alisworld.app.data.ApiClient
import com.alisworld.app.data.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PositionsUiState(
    val isLoading: Boolean = false,
    val positions: List<Position> = emptyList(),
    val error: String? = null
)

class PositionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PositionsUiState())
    val uiState: StateFlow<PositionsUiState> = _uiState.asStateFlow()
    
    fun loadPositions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            ApiClient.instance.getPositions()
                .onSuccess { positions ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        positions = positions,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    )
                }
        }
    }
    
    fun closePosition(ticket: Long) {
        viewModelScope.launch {
            ApiClient.instance.closePosition(ticket)
                .onSuccess {
                    // Reload positions after close command
                    loadPositions()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to close position"
                    )
                }
        }
    }
}
