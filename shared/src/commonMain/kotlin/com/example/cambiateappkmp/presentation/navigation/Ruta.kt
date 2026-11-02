package com.example.cambiateappkmp.presentation.navigation

sealed interface Ruta {
    data object Login : Ruta
    data object Registro : Ruta
}