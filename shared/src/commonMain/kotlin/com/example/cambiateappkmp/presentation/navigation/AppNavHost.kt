package com.example.cambiateappkmp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cambiateappkmp.presentation.auth.LoginRoute
import com.example.cambiateappkmp.presentation.auth.RegisterRoute
import com.example.cambiateappkmp.presentation.components.PantallaCarga
import com.example.cambiateappkmp.presentation.home.HomeProvisoriaScreen
import com.example.cambiateappkmp.presentation.session.SessionUiState
import com.example.cambiateappkmp.presentation.session.SessionViewModel
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun AppNavHost(
    sessionViewModel: SessionViewModel = koinViewModel(),
    navegacion: NavegacionViewModel = koinViewModel(),
) {
    val sesion by sessionViewModel.estado.collectAsState()

    LaunchedEffect(sesion) {
        if (sesion is SessionUiState.ConSesion) navegacion.reiniciar()
    }

    when (val estado = sesion) {
        SessionUiState.Cargando -> PantallaCarga()

        is SessionUiState.ConSesion -> HomeProvisoriaScreen(
            email = estado.email,
            onCerrarSesion = sessionViewModel::cerrarSesion,
        )

        SessionUiState.SinSesion -> when (navegacion.actual) {
            Ruta.Login -> LoginRoute(
                onIrARegistro = { navegacion.ir(Ruta.Registro) },
            )

            Ruta.Registro -> RegisterRoute(
                onIrALogin = { navegacion.volver() },
            )
        }
    }
}