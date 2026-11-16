package com.example.cambiateappkmp.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.cambiateappkmp.presentation.ajustes.AjustesRoute
import com.example.cambiateappkmp.presentation.components.PantallaCarga
import com.example.cambiateappkmp.presentation.mapa.MapaRoute
import com.example.cambiateappkmp.presentation.notificaciones.NotificacionesRoute
import kotlinx.coroutines.launch

/*
 * Conexión de las pantallas de la semana 3 (Mapa, Ajustes y Notificaciones) con la navegación actual.
 * Está todo en este archivo a propósito: cuando entre la navegación definitiva, alcanza con reemplazar
 * PantallasSemana3 por las rutas nuevas, sacar los accesos del armario y borrar este archivo.
 */

data object RutaMapa : Ruta

data object RutaAjustes : Ruta

data object RutaNotificaciones : Ruta

/** Dibuja la pantalla que corresponde a la ruta. Las rutas que no son de la semana 3 muestran la pantalla de carga. */
@Composable
fun PantallasSemana3(ruta: Ruta, navegacion: NavegacionViewModel) {
    when (ruta) {
        RutaMapa -> MapaConBarra(onVolver = { navegacion.volver() })

        RutaAjustes -> AjustesRoute(
            onVolver = { navegacion.volver() },
            onNotificacionesClick = { navegacion.ir(RutaNotificaciones) },
        )

        RutaNotificaciones -> NotificacionesRoute(
            onVolver = { navegacion.volver() },
        )

        else -> PantallaCarga()
    }
}

/** El Mapa no trae barra superior propia: acá le ponemos el título y el botón "Volver". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapaConBarra(onVolver: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mapa", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            MapaRoute(
                onSugerir = {
                    // TODO: conectar con el agente de sugerencias de outfits (Lourdes) y pasarle el clima, que puede ser null.
                    scope.launch { snackbarHostState.showSnackbar("Pronto: sugerencias de outfit") }
                },
            )
        }
    }
}
