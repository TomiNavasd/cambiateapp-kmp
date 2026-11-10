package com.example.cambiateappkmp.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage

/** Muestra la foto de una prenda desde su URL (Coil + Ktor). Si no hay URL o falla, muestra un texto. */
@Composable
fun FotoPrenda(url: String, modifier: Modifier = Modifier) {
    if (url.isBlank()) {
        MensajeFoto(texto = "Sin foto", modifier = modifier)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = "Foto de la prenda",
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        error = {
            MensajeFoto(texto = "No se pudo cargar la foto", modifier = Modifier.fillMaxSize())
        },
    )
}

@Composable
private fun MensajeFoto(texto: String, modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
