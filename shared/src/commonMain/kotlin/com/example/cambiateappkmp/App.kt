package com.example.cambiateappkmp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cambiateappkmp.domain.model.TemaApp
import com.example.cambiateappkmp.domain.notification.Notificador
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import com.example.cambiateappkmp.presentation.components.ContenedorAnchoMaximo
import com.example.cambiateappkmp.presentation.navigation.AppNavHost
import com.example.cambiateappkmp.presentation.notificaciones.notificacionesDisponibles
import com.example.cambiateappkmp.ui.theme.CambiateAppTheme
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

@Composable
fun App() {
    val preferencias: PreferenciasRepository = koinInject()
    val notificador: Notificador = koinInject()
    val tema by preferencias.tema.collectAsState(initial = TemaApp.SISTEMA)

    val oscuro = when (tema) {
        TemaApp.SISTEMA -> isSystemInDarkTheme()
        TemaApp.CLARO -> false
        TemaApp.OSCURO -> true
    }

    // Si los recordatorios están activados, nos aseguramos de que la tarea diaria siga programada (solo donde hay).
    LaunchedEffect(Unit) {
        if (!notificacionesDisponibles) return@LaunchedEffect
        val config = preferencias.notificaciones.first()
        if (config.activadas) notificador.programarDiaria(config.hora, config.minuto)
    }

    CambiateAppTheme(darkTheme = oscuro) {
        // En ventanas anchas (escritorio) el contenido se centra y no pasa de 1100 dp; en un celular no cambia nada.
        ContenedorAnchoMaximo {
            AppNavHost()
        }
    }
}
