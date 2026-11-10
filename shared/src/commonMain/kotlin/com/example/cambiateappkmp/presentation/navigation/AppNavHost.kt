package com.example.cambiateappkmp.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cambiateappkmp.presentation.auth.LoginRoute
import com.example.cambiateappkmp.presentation.auth.RegisterRoute
import com.example.cambiateappkmp.presentation.components.BotonPrincipal
import com.example.cambiateappkmp.presentation.components.PantallaCarga
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

    // Cada vez que cambia la sesión, la navegación vuelve a empezar desde la pantalla que corresponde.
    LaunchedEffect(sesion) {
        when (sesion) {
            is SessionUiState.ConSesion -> navegacion.reiniciar(Ruta.Armario)
            SessionUiState.SinSesion -> navegacion.reiniciar(Ruta.Login)
            SessionUiState.Cargando -> Unit
        }
    }

    when (sesion) {
        SessionUiState.Cargando -> PantallaCarga()

        is SessionUiState.ConSesion -> when (val ruta = navegacion.actual) {
            Ruta.Armario -> WardrobeListRoute(
                onPrendaClick = { id -> navegacion.ir(Ruta.DetallePrenda(id)) },
                onNuevaClick = { navegacion.ir(Ruta.AltaEdicion(null)) },
                onCerrarSesion = sessionViewModel::cerrarSesion,
            )

            // Provisorias: se reemplazan en las etapas 4 (detalle) y 5 (alta y edición).
            is Ruta.DetallePrenda -> PantallaProvisoria(
                titulo = "Detalle de la prenda ${ruta.id}",
                onVolver = { navegacion.volver() },
            )

            is Ruta.AltaEdicion -> PantallaProvisoria(
                titulo = if (ruta.id == null) "Nueva prenda" else "Editar prenda ${ruta.id}",
                onVolver = { navegacion.volver() },
            )

            // Un instante, hasta que el LaunchedEffect de arriba reinicia la pila.
            else -> PantallaCarga()
        }

        SessionUiState.SinSesion -> when (navegacion.actual) {
            Ruta.Registro -> RegisterRoute(
                onIrALogin = { navegacion.volver() },
            )

            // Login, y cualquier ruta de sesión que quede un instante al cerrarla.
            else -> LoginRoute(
                onIrARegistro = { navegacion.ir(Ruta.Registro) },
            )
        }
    }
}

@Composable
private fun PantallaProvisoria(
    titulo: String,
    onVolver: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Próximamente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BotonPrincipal(texto = "Volver", alClick = onVolver)
        }
    }
}