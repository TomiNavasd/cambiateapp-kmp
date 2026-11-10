package com.example.cambiateappkmp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cambiateappkmp.presentation.altaedicion.AltaEdicionRoute
import com.example.cambiateappkmp.presentation.auth.LoginRoute
import com.example.cambiateappkmp.presentation.auth.RegisterRoute
import com.example.cambiateappkmp.presentation.components.PantallaCarga
import com.example.cambiateappkmp.presentation.detalle.DetallePrendaRoute
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

    ManejarAtras(habilitado = navegacion.pila.size > 1) {
        navegacion.volver()
    }

    when (sesion) {
        SessionUiState.Cargando -> PantallaCarga()

        is SessionUiState.ConSesion -> when (val ruta = navegacion.actual) {
            Ruta.Armario -> WardrobeListRoute(
                onPrendaClick = { id -> navegacion.ir(Ruta.DetallePrenda(id)) },
                onNuevaClick = { navegacion.ir(Ruta.AltaEdicion(null)) },
                onCerrarSesion = sessionViewModel::cerrarSesion,
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

            else -> PantallaCarga()
        }

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