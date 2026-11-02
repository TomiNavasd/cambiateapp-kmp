package com.example.cambiateappkmp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cambiateappkmp.domain.usecase.ObserveAuthStateUseCase
import com.example.cambiateappkmp.domain.usecase.SignOutUseCase
import com.example.cambiateappkmp.presentation.auth.LoginRoute
import com.example.cambiateappkmp.presentation.auth.RegisterRoute
import com.example.cambiateappkmp.presentation.components.BotonPrincipal
import com.example.cambiateappkmp.ui.theme.CambiateAppTheme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun App() {
    val observarSesion: ObserveAuthStateUseCase = koinInject()
    val cerrarSesion: SignOutUseCase = koinInject()
    val usuario by observarSesion().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var enRegistro by remember { mutableStateOf(false) }

    CambiateAppTheme(darkTheme = isSystemInDarkTheme()) {
        val sesion = usuario
        if (sesion != null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().safeContentPadding().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Sesión iniciada",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(text = sesion.email, color = MaterialTheme.colorScheme.onBackground)
                    BotonPrincipal(
                        texto = "Cerrar sesión",
                        alClick = { scope.launch { cerrarSesion() } },
                    )
                }
            }
        } else if (enRegistro) {
            RegisterRoute(onIrALogin = { enRegistro = false })
        } else {
            LoginRoute(onIrARegistro = { enRegistro = true })
        }
    }
}