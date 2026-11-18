package com.example.cambiateappkmp.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage

/**
 * Muestra la foto de una prenda desde su URL (Coil + Ktor). Si no hay URL o falla, muestra un texto.
 *
 * En pantallas anchas (escritorio) pide a Cloudinary la foto ya redimensionada a [anchoPx] (para que se vea nítida y no
 * pixelada al agrandarla) y la dibuja con filtro de calidad alta. En un celular se carga igual que antes.
 * [recortar] = true la pide cuadrada (grillas); false, entera y con sus proporciones (detalle). La URL guardada no cambia.
 */
@Composable
fun FotoPrenda(
    url: String,
    modifier: Modifier = Modifier,
    anchoPx: Int = 400,
    recortar: Boolean = true,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url.isBlank()) {
        MensajeFoto(texto = "Sin foto", modifier = modifier)
        return
    }
    val ancha = esPantallaAncha()
    val urlAMostrar = remember(url, anchoPx, recortar, ancha) {
        if (ancha) url.conTamanioCloudinary(anchoPx, recortar) else url
    }
    SubcomposeAsyncImage(
        model = urlAMostrar,
        contentDescription = "Foto de la prenda",
        contentScale = contentScale,
        filterQuality = if (ancha) FilterQuality.High else FilterQuality.Low,
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
