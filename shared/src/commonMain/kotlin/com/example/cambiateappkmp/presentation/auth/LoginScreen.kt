package com.example.cambiateappkmp.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cambiateappkmp.presentation.components.BotonPrincipal
import com.example.cambiateappkmp.presentation.components.CampoTexto
import com.example.cambiateappkmp.presentation.components.MensajeError

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    onIrARegistro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "CambiateApp",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Iniciá sesión para ver tu armario",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            state.errorGeneral?.let { MensajeError(mensaje = it) }

            CampoTexto(
                valor = state.email,
                alCambiar = { onEvent(LoginUiEvent.EmailCambiado(it)) },
                etiqueta = "Email",
                error = state.errorEmail,
                tipoTeclado = KeyboardType.Email,
                habilitado = !state.cargando,
            )
            CampoTexto(
                valor = state.contrasena,
                alCambiar = { onEvent(LoginUiEvent.ContrasenaCambiada(it)) },
                etiqueta = "Contraseña",
                error = state.errorContrasena,
                esContrasena = true,
                habilitado = !state.cargando,
            )

            BotonPrincipal(
                texto = "Ingresar",
                alClick = { onEvent(LoginUiEvent.Ingresar) },
                cargando = state.cargando,
            )

            TextButton(onClick = onIrARegistro, enabled = !state.cargando) {
                Text("¿No tenés cuenta? Registrate")
            }
        }
    }
}