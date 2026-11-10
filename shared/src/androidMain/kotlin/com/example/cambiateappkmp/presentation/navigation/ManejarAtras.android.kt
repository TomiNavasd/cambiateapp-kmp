package com.example.cambiateappkmp.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun ManejarAtras(habilitado: Boolean, alAtras: () -> Unit) {
    BackHandler(enabled = habilitado, onBack = alAtras)
}