package com.example.cambiateappkmp.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation


@Composable
fun CampoTexto(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    esContrasena: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    habilitado: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        modifier = modifier.fillMaxWidth(),
        enabled = habilitado,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        visualTransformation = if (esContrasena && !visible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado,
        ),
        trailingIcon = if (esContrasena) {
            {
                TextButton(onClick = { visible = !visible }) {
                    Text(if (visible) "Ocultar" else "Mostrar")
                }
            }
        } else {
            null
        },
        shape = MaterialTheme.shapes.medium,
    )
}