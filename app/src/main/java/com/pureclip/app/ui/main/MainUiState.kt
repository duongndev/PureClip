package com.pureclip.app.ui.main

sealed interface MainUiState {
    data object Idle : MainUiState
    data object Loading : MainUiState
    data object NoInternet : MainUiState
    data class Error(val message: String? = null) : MainUiState
}
