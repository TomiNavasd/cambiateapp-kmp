package com.example.cambiateappkmp.presentation.auth

data class LoginUiState(
    val email: String = "",
    val contrasena: String = "",
    val errorEmail: String? = null,
    val errorContrasena: String? = null,
    val errorGeneral: String? = null,
    val cargando: Boolean = false,
)

sealed interface LoginUiEvent {
    data class EmailCambiado(val valor: String) : LoginUiEvent
    data class ContrasenaCambiada(val valor: String) : LoginUiEvent
    data object Ingresar : LoginUiEvent
}