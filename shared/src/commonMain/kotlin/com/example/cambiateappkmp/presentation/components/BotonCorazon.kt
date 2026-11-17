package com.example.cambiateappkmp.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Corazón dibujado a mano (el proyecto no tiene librería de íconos): relleno si está marcado, solo borde si no. */
@Composable
fun BotonCorazon(
    relleno: Boolean,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (relleno) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = onClick, modifier = modifier.semantics { contentDescription = descripcion }) {
        Corazon(relleno = relleno, color = color)
    }
}

@Composable
private fun Corazon(relleno: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val camino = caminoDeCorazon(size)
        if (relleno) {
            drawPath(camino, color = color, style = Fill)
        } else {
            drawPath(camino, color = color, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

private fun caminoDeCorazon(tamano: Size): Path {
    val w = tamano.width
    val h = tamano.height
    return Path().apply {
        moveTo(w * 0.50f, h * 0.90f)
        cubicTo(w * 0.10f, h * 0.60f, w * 0.02f, h * 0.35f, w * 0.15f, h * 0.20f)
        cubicTo(w * 0.28f, h * 0.05f, w * 0.46f, h * 0.12f, w * 0.50f, h * 0.28f)
        cubicTo(w * 0.54f, h * 0.12f, w * 0.72f, h * 0.05f, w * 0.85f, h * 0.20f)
        cubicTo(w * 0.98f, h * 0.35f, w * 0.90f, h * 0.60f, w * 0.50f, h * 0.90f)
        close()
    }
}
