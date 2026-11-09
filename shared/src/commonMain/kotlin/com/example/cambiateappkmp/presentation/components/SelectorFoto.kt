package com.example.cambiateappkmp.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun SelectorFoto(
    fotoUrl: String,
    onFotoSubida: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FotoPrenda(url = fotoUrl, modifier = Modifier)
        OutlinedButton(
            onClick = { onFotoSubida("https://picsum.photos/seed/cambiateapp/400/400") },
        ) {
            Text(if (fotoUrl.isBlank()) "Agregar foto (provisorio)" else "Cambiar foto (provisorio)")
        }
    }
}