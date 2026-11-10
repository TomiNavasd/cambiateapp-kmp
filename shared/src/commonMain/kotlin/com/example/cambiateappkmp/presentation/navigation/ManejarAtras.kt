package com.example.cambiateappkmp.presentation.navigation

import androidx.compose.runtime.Composable


@Composable
expect fun ManejarAtras(habilitado: Boolean, alAtras: () -> Unit)