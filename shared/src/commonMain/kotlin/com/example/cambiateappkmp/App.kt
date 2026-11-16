package com.example.cambiateappkmp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cambiateappkmp.domain.model.TemaApp
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import com.example.cambiateappkmp.presentation.navigation.AppNavHost
import com.example.cambiateappkmp.ui.theme.CambiateAppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val preferencias: PreferenciasRepository = koinInject()
    val tema by preferencias.tema.collectAsState(initial = TemaApp.SISTEMA)

    val oscuro = when (tema) {
        TemaApp.SISTEMA -> isSystemInDarkTheme()
        TemaApp.CLARO -> false
        TemaApp.OSCURO -> true
    }

    CambiateAppTheme(darkTheme = oscuro) {
        AppNavHost()
    }
}
