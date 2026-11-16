package com.example.cambiateappkmp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.cambiateappkmp.domain.model.TemaApp
import com.example.cambiateappkmp.domain.notification.Notificador
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import com.example.cambiateappkmp.presentation.avisos.AvisoEnApp
import com.example.cambiateappkmp.presentation.avisos.AvisosEnApp
import com.example.cambiateappkmp.presentation.navigation.AppNavHost
import com.example.cambiateappkmp.ui.theme.CambiateAppTheme
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

@Composable
fun App() {
    val preferencias: PreferenciasRepository = koinInject()
    val notificador: Notificador = koinInject()
    val avisosEnApp: AvisosEnApp = koinInject()
    val tema by preferencias.tema.collectAsState(initial = TemaApp.SISTEMA)

    val oscuro = when (tema) {
        TemaApp.SISTEMA -> isSystemInDarkTheme()
        TemaApp.CLARO -> false
        TemaApp.OSCURO -> true
    }

    // Si los recordatorios están activados, nos aseguramos de que la tarea diaria siga programada.
    LaunchedEffect(Unit) {
        val config = preferencias.notificaciones.first()
        if (config.activadas) notificador.programarDiaria(config.hora, config.minuto)
    }

    // Aviso dentro de la app: se usa cuando el sistema no puede mostrar una notificación.
    var aviso by remember { mutableStateOf<AvisoEnApp?>(null) }
    LaunchedEffect(Unit) { avisosEnApp.avisos.collect { aviso = it } }

    CambiateAppTheme(darkTheme = oscuro) {
        AppNavHost()

        aviso?.let { actual ->
            AlertDialog(
                onDismissRequest = { aviso = null },
                title = { Text(actual.titulo) },
                text = { Text(actual.mensaje) },
                confirmButton = { TextButton(onClick = { aviso = null }) { Text("Aceptar") } },
            )
        }
    }
}
