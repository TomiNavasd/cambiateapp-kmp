package com.example.cambiateappkmp.presentation.navigation

sealed interface Ruta {
    data object Login : Ruta
    data object Registro : Ruta

    data object Armario : Ruta
    data class DetallePrenda(val id: String) : Ruta

    data class AltaEdicion(val id: String?) : Ruta
}