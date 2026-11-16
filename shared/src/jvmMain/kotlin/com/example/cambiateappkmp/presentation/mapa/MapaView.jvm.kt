package com.example.cambiateappkmp.presentation.mapa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cambiateappkmp.domain.model.LocalRopa
import com.example.cambiateappkmp.domain.model.Ubicacion
import kotlin.math.abs
import kotlin.math.floor

private const val ZOOM_INICIAL = 15

/** Desde este zoom se escriben los nombres de los locales al lado de su marcador. */
private const val ZOOM_PARA_NOMBRES = 16

private const val ATRIBUCION = "© OpenStreetMap contributors, SRTM | Estilo: © OpenTopoMap (CC-BY-SA)"

/**
 * Mapa de escritorio dibujado con Compose: baja los tiles de OpenTopoMap (la misma fuente que Android),
 * se desplaza arrastrando y se acerca o aleja con la rueda del mouse o con los botones + y −.
 */
@Composable
actual fun MapaView(
    ubicacion: Ubicacion?,
    locales: List<LocalRopa>,
    modifier: Modifier,
) {
    val cliente = remember { crearClienteTiles() }
    DisposableEffect(cliente) { onDispose { cliente.close() } }

    val alcance = rememberCoroutineScope()
    val cargador = remember(cliente) { CargadorDeTiles(cliente, alcance) }

    var centroLat by remember { mutableStateOf(ubicacion?.lat ?: -34.6037) }
    var centroLng by remember { mutableStateOf(ubicacion?.lng ?: -58.3816) }
    var zoom by remember { mutableStateOf(ZOOM_INICIAL) }
    var tamano by remember { mutableStateOf(IntSize.Zero) }

    // Cuando llega una ubicación nueva, el mapa se centra ahí.
    LaunchedEffect(ubicacion) {
        ubicacion?.let {
            centroLat = it.lat
            centroLng = it.lng
        }
    }

    // Se piden los tiles que se ven; los más cercanos al centro primero.
    val visibles = remember(centroLat, centroLng, zoom, tamano) {
        clavesVisibles(tileX(centroLng, zoom), tileY(centroLat, zoom), zoom, tamano.width, tamano.height)
    }
    LaunchedEffect(visibles) { visibles.forEach(cargador::solicitar) }

    val medidor = rememberTextMeasurer()
    val colorFondo = MaterialTheme.colorScheme.surfaceVariant
    val colorUsuario = MaterialTheme.colorScheme.primary
    val colorLocal = MaterialTheme.colorScheme.error
    val estiloTexto = TextStyle(color = Color.Black, fontSize = 12.sp)

    Box(modifier = modifier.clipToBounds().background(colorFondo)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { tamano = it }
                .pointerInput(Unit) {
                    detectDragGestures { cambio, arrastre ->
                        cambio.consume()
                        // Arrastrar el mapa hacia un lado mueve el centro hacia el lado contrario.
                        val x = tileX(centroLng, zoom) - arrastre.x / TAMANO_TILE
                        val y = tileY(centroLat, zoom) - arrastre.y / TAMANO_TILE
                        centroLng = ((lngDeTileX(x, zoom) + 540.0) % 360.0) - 180.0
                        centroLat = latDeTileY(y, zoom).coerceIn(-85.0, 85.0)
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        var acumulado = 0f
                        while (true) {
                            val evento = awaitPointerEvent()
                            if (evento.type == PointerEventType.Scroll) {
                                // Una vuelta de rueda cambia un nivel de zoom; los trackpads mandan valores chicos y se acumulan.
                                acumulado += evento.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (abs(acumulado) >= 1f) {
                                    zoom = (if (acumulado < 0f) zoom + 1 else zoom - 1).coerceIn(ZOOM_MINIMO, ZOOM_MAXIMO)
                                    acumulado = 0f
                                }
                                evento.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
        ) {
            val centroX = tileX(centroLng, zoom)
            val centroY = tileY(centroLat, zoom)
            val mitadAncho = size.width / 2f
            val mitadAlto = size.height / 2f
            val cantidad = 1 shl zoom

            fun aPantalla(lat: Double, lng: Double) = Offset(
                x = mitadAncho + ((tileX(lng, zoom) - centroX) * TAMANO_TILE).toFloat(),
                y = mitadAlto + ((tileY(lat, zoom) - centroY) * TAMANO_TILE).toFloat(),
            )

            // Los tiles que ya llegaron. Los que faltan dejan ver el color de fondo hasta que se descargan.
            val desdeX = floor(centroX - mitadAncho / TAMANO_TILE).toInt()
            val hastaX = floor(centroX + mitadAncho / TAMANO_TILE).toInt()
            val desdeY = floor(centroY - mitadAlto / TAMANO_TILE).toInt().coerceAtLeast(0)
            val hastaY = floor(centroY + mitadAlto / TAMANO_TILE).toInt().coerceAtMost(cantidad - 1)
            for (tx in desdeX..hastaX) {
                for (ty in desdeY..hastaY) {
                    val imagen = cargador.tiles[ClaveTile(zoom, ((tx % cantidad) + cantidad) % cantidad, ty)] ?: continue
                    drawImage(
                        image = imagen,
                        topLeft = Offset(
                            x = mitadAncho + ((tx - centroX) * TAMANO_TILE).toFloat(),
                            y = mitadAlto + ((ty - centroY) * TAMANO_TILE).toFloat(),
                        ),
                    )
                }
            }

            // Marcadores de los locales.
            locales.forEach { local ->
                val punto = aPantalla(local.lat, local.lng)
                drawCircle(Color.White, radius = 10f, center = punto)
                drawCircle(colorLocal, radius = 7f, center = punto)
                if (zoom >= ZOOM_PARA_NOMBRES) {
                    drawText(medidor.measure(local.nombre, estiloTexto), topLeft = punto + Offset(12f, -9f))
                }
            }

            // El marcador "Estás acá" va al final, para que quede encima de todo.
            if (ubicacion != null) {
                val punto = aPantalla(ubicacion.lat, ubicacion.lng)
                drawCircle(Color.White, radius = 13f, center = punto)
                drawCircle(colorUsuario, radius = 9f, center = punto)
                drawText(medidor.measure("Estás acá", estiloTexto), topLeft = punto + Offset(14f, -9f))
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallFloatingActionButton(onClick = { zoom = (zoom + 1).coerceAtMost(ZOOM_MAXIMO) }) { Text("+") }
            SmallFloatingActionButton(onClick = { zoom = (zoom - 1).coerceAtLeast(ZOOM_MINIMO) }) { Text("−") }
        }

        // La licencia del mapa exige mostrar esta atribución.
        Text(
            text = ATRIBUCION,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(4.dp),
        )
    }
}
