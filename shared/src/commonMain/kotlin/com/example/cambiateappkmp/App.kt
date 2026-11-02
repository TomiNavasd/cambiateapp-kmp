package com.example.cambiateappkmp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.example.cambiateappkmp.presentation.navigation.AppNavHost
import com.example.cambiateappkmp.ui.theme.CambiateAppTheme

@Composable
fun App() {
    CambiateAppTheme(darkTheme = isSystemInDarkTheme()) {
        AppNavHost()
    }
}