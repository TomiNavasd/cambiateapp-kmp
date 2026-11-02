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
fun RegisterScreen(
    state: RegisterUiState,
    onEvent: (RegisterUiEvent) -> Unit,
    onIrALogin: () -> Unit,
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
                text = "Crear cuenta",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Registrate para armar tu armario digital",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            state.errorGeneral?.let { MensajeError(mensaje = it) }

            CampoTexto(
                valor = state.email,
                alCambiar = { onEvent(RegisterUiEvent.EmailCambiado(it)) },
                etiqueta = "Email",
                error = state.errorEmail,
                tipoTeclado = KeyboardType.Email,
                habilitado = !state.cargando,
            )
            CampoTexto(
                valor = state.contrasena,
                alCambiar = { onEvent(RegisterUiEvent.ContrasenaCambiada(it)) },
                etiqueta = "Contraseña (mínimo 6 caracteres)",
                error = state.errorContrasena,
                esContrasena = true,
                habilitado = !state.cargando,
            )

            BotonPrincipal(
                texto = "Registrarme",
                alClick = { onEvent(RegisterUiEvent.Registrar) },
                cargando = state.cargando,
            )

            TextButton(onClick = onIrALogin, enabled = !state.cargando) {
                Text("¿Ya tenés cuenta? Ingresá")
            }
        }
    }
}