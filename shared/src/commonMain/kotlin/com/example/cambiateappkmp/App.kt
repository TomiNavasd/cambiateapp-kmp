package com.example.cambiateappkmp

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cambiateappkmp.presentation.components.BotonPrincipal
import com.example.cambiateappkmp.presentation.components.CampoTexto
import com.example.cambiateappkmp.presentation.components.MensajeError
import com.example.cambiateappkmp.ui.theme.CambiateAppTheme

@Composable
fun App() {
    val sistemaOscuro = isSystemInDarkTheme()
    var oscuro by remember { mutableStateOf(sistemaOscuro) }
    var email by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    CambiateAppTheme(darkTheme = oscuro) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Modo oscuro",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Switch(checked = oscuro, onCheckedChange = { oscuro = it })
            }

            Text(
                text = "CambiateApp",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Prueba del tema y los componentes",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MensajeError(mensaje = "Email o contraseña incorrectos")

            CampoTexto(
                valor = email,
                alCambiar = { email = it },
                etiqueta = "Email",
                tipoTeclado = KeyboardType.Email,
            )
            CampoTexto(
                valor = contrasena,
                alCambiar = { contrasena = it },
                etiqueta = "Contraseña",
                esContrasena = true,
                error = if (contrasena.isNotEmpty() && contrasena.length < 6) {
                    "Mínimo 6 caracteres"
                } else {
                    null
                },
            )

            BotonPrincipal(texto = "Ingresar", alClick = {})
            BotonPrincipal(texto = "Ingresar", alClick = {}, cargando = true)
        }
    }
}