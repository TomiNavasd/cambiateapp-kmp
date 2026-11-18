package com.example.cambiateappkmp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Ajustes para pantallas anchas (escritorio y tablets). En un celular nada de esto cambia el aspecto:
 * todo se activa recién a partir de UMBRAL_PANTALLA_ANCHA.
 */

/** A partir de este ancho de ventana la pantalla se considera "ancha". Un celular en vertical no llega. */
val UMBRAL_PANTALLA_ANCHA: Dp = 600.dp

/** Ancho máximo de la foto del detalle de una prenda. */
val ANCHO_MAXIMO_FOTO_DETALLE: Dp = 420.dp

/**
 * Ancho máximo del contenido de las pantallas que no son grilla (formularios, detalle, ajustes), que se leen mal si se
 * estiran a toda la ventana. Solo se limita el contenido: el fondo y las barras siguen ocupando todo el ancho.
 */
val ANCHO_MAXIMO_FORMULARIO: Dp = 720.dp

@Composable
fun esPantallaAncha(): Boolean {
    val densidad = LocalDensity.current.density
    val anchoPx = LocalWindowInfo.current.containerSize.width
    return anchoPx / densidad >= UMBRAL_PANTALLA_ANCHA.value
}

/** Centra el contenido y lo limita a [anchoMaximo]. Si la ventana es más angosta, no hace nada. */
@Composable
fun ContenedorAnchoMaximo(
    modifier: Modifier = Modifier,
    anchoMaximo: Dp = ANCHO_MAXIMO_FORMULARIO,
    contenido: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.widthIn(max = anchoMaximo).fillMaxSize()) {
            contenido()
        }
    }
}

/**
 * Columnas de una grilla: en un celular (ancho disponible menor a [umbralAncho]) siempre [columnasEnCelular], como antes;
 * en pantallas anchas tantas como entren con celdas de al menos [minSize].
 */
@Immutable
data class ColumnasResponsivas(
    val minSize: Dp,
    val columnasEnCelular: Int = 2,
    val umbralAncho: Dp = UMBRAL_PANTALLA_ANCHA,
) : GridCells {

    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> =
        repartirColumnas(
            ancho = availableSize,
            separacion = spacing,
            cantidad = cantidadDeColumnas(availableSize, spacing, minSize.roundToPx(), columnasEnCelular, umbralAncho.roundToPx()),
        )
}

internal fun cantidadDeColumnas(ancho: Int, separacion: Int, minimo: Int, columnasEnCelular: Int, umbral: Int): Int =
    if (ancho < umbral) columnasEnCelular else maxOf((ancho + separacion) / (minimo + separacion), 1)

/** Reparte el ancho en [cantidad] celdas iguales (el sobrante, de a un píxel, en las primeras). */
internal fun repartirColumnas(ancho: Int, separacion: Int, cantidad: Int): List<Int> {
    val total = ancho - separacion * (cantidad - 1)
    val base = total / cantidad
    val resto = total % cantidad
    return List(cantidad) { base + if (it < resto) 1 else 0 }
}
