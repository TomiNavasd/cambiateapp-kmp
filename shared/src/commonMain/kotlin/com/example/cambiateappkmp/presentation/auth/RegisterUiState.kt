package com.example.cambiateappkmp.presentation.auth

data class RegisterUiState(
    val email: String = "",
    val contrasena: String = "",
    val errorEmail: String? = null,
    val errorContrasena: String? = null,
    val errorGeneral: String? = null,
    val cargando: Boolean = false,
)

sealed interface RegisterUiEvent {
    data class EmailCambiado(val valor: String) : RegisterUiEvent
    data class ContrasenaCambiada(val valor: String) : RegisterUiEvent
    data object Registrar : RegisterUiEvent
}