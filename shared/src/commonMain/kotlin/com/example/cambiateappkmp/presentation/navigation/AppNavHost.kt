package com.example.cambiateappkmp.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.presentation.ajustes.AjustesRoute
import com.example.cambiateappkmp.presentation.altaedicion.AltaEdicionRoute
import com.example.cambiateappkmp.presentation.auth.LoginRoute
import com.example.cambiateappkmp.presentation.auth.RegisterRoute
import com.example.cambiateappkmp.presentation.components.PantallaCarga
import com.example.cambiateappkmp.presentation.detalle.DetallePrendaRoute
import com.example.cambiateappkmp.presentation.mapa.MapaRoute
import com.example.cambiateappkmp.presentation.notificaciones.NotificacionesRoute
import com.example.cambiateappkmp.presentation.outfits.OutfitsRoute
import com.example.cambiateappkmp.presentation.session.SessionUiState
import com.example.cambiateappkmp.presentation.session.SessionViewModel
import com.example.cambiateappkmp.presentation.wardrobe.WardrobeListRoute
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun AppNavHost(
    sessionViewModel: SessionViewModel = koinViewModel(),
    navegacion: NavegacionViewModel = koinViewModel(),
) {
    val sesion by sessionViewModel.estado.collectAsState()

    LaunchedEffect(sesion) {
        when (sesion) {
            is SessionUiState.ConSesion -> navegacion.reiniciar(Ruta.Armario)
            SessionUiState.SinSesion -> navegacion.reiniciar(Ruta.Login)
            SessionUiState.Cargando -> Unit
        }
    }

    // Desde una pestaña raíz (la última de la pila) el Atrás no se intercepta: lo resuelve el sistema.
    ManejarAtras(habilitado = navegacion.pila.size > 1) {
        navegacion.volver()
    }

    when (sesion) {
        SessionUiState.Cargando -> PantallaCarga()

        is SessionUiState.ConSesion -> ConSesion(sessionViewModel, navegacion)

        SessionUiState.SinSesion -> when (navegacion.actual) {
            Ruta.Registro -> RegisterRoute(
                onIrALogin = { navegacion.volver() },
            )

            else -> LoginRoute(
                onIrARegistro = { navegacion.ir(Ruta.Registro) },
            )
        }
    }
}

@Composable
private fun ConSesion(sessionViewModel: SessionViewModel, navegacion: NavegacionViewModel) {
    val actual = navegacion.actual

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (actual.muestraBarraInferior) {
                BarraInferior(rutaActual = actual, onPestana = navegacion::irAPestana)
            }
        },
    ) { padding ->
        // Las pantallas traen su propio Scaffold: le avisamos que la barra ya consumió ese espacio.
        Pantalla(
            ruta = actual,
            sessionViewModel = sessionViewModel,
            navegacion = navegacion,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        )
    }
}

@Composable
private fun Pantalla(
    ruta: Ruta,
    sessionViewModel: SessionViewModel,
    navegacion: NavegacionViewModel,
    modifier: Modifier,
) {
    Box(modifier = modifier) {
        when (ruta) {
            Ruta.Armario -> WardrobeListRoute(
                onPrendaClick = { id -> navegacion.ir(Ruta.DetallePrenda(id)) },
                onNuevaClick = { navegacion.ir(Ruta.AltaEdicion(null)) },
                onCerrarSesion = sessionViewModel::cerrarSesion,
                onAjustesClick = { navegacion.ir(Ruta.Ajustes) },
            )

            is Ruta.Outfits -> OutfitsRoute(clima = ruta.clima)

            Ruta.Mapa -> MapaConBarra(
                onVolver = { navegacion.volver() },
                onSugerir = { clima -> navegacion.irAPestana(Ruta.Outfits(clima)) },
            )

            Ruta.Ajustes -> AjustesRoute(
                onVolver = { navegacion.volver() },
                onNotificacionesClick = { navegacion.ir(Ruta.Notificaciones) },
            )

            Ruta.Notificaciones -> NotificacionesRoute(
                onVolver = { navegacion.volver() },
            )

            is Ruta.DetallePrenda -> DetallePrendaRoute(
                id = ruta.id,
                onVolver = { navegacion.volver() },
                onEditarClick = { navegacion.ir(Ruta.AltaEdicion(ruta.id)) },
            )

            is Ruta.AltaEdicion -> AltaEdicionRoute(
                id = ruta.id,
                onVolver = { navegacion.volver() },
                onGuardada = { navegacion.volver() },
            )

            Ruta.Login, Ruta.Registro -> PantallaCarga()
        }
    }
}

/** El Mapa no trae barra superior propia: acá va el título y "Volver", que vuelve al Armario como en la nativa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapaConBarra(onVolver: () -> Unit, onSugerir: (ClimaActual?) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mapa", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            MapaRoute(onSugerir = onSugerir)
        }
    }
}
