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
    val closingTicket: Long? = null,
    val notice: String? = null,
    val error: String? = null
)

class PositionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PositionsUiState())
    val uiState: StateFlow<PositionsUiState> = _uiState.asStateFlow()

    fun loadPositions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            ApiClient.instance.getPositions()
                .onSuccess { positions -> _uiState.value = _uiState.value.copy(isLoading = false, positions = positions, error = null) }
                .onFailure { error -> 
                    android.util.Log.e("PositionsViewModel", "Load positions failed: ${error.message}", error)
                    val errorMsg = when {
                        error.message?.contains("CommandResponse") == true -> 
                            "API response format error. Try force-closing the app and reopening."
                        else -> error.message ?: "Unable to load positions"
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
        }
    }

    fun closePosition(ticket: Long) {
        if (_uiState.value.closingTicket != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(closingTicket = ticket, error = null, notice = null)
            ApiClient.instance.closePosition(ticket)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        closingTicket = null,
                        notice = "Close command #${response.commandId} queued. MT5 will confirm after execution."
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(closingTicket = null, error = error.message ?: "Failed to queue close command")
                }
        }
    }

    fun clearNotice() { _uiState.value = _uiState.value.copy(notice = null) }
}
