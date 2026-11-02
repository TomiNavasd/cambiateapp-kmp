package com.example.cambiateappkmp.presentation.session

sealed interface SessionUiState {
    data object Cargando : SessionUiState

    data object SinSesion : SessionUiState

    data class ConSesion(val email: String) : SessionUiState
}