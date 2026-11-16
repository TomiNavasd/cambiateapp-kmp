package com.example.cambiateappkmp.presentation.mapa

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

/** Cada imagen del mapa (tile) mide 256 x 256 píxeles. */
internal const val TAMANO_TILE = 256

internal const val ZOOM_MINIMO = 3
internal const val ZOOM_MAXIMO = 17

/** Latitud máxima que representa la proyección Web Mercator. */
private const val LATITUD_MAXIMA = 85.0511

/** Identifica un tile: nivel de zoom (z) y posición (x, y) en la grilla de ese nivel. */
internal data class ClaveTile(val z: Int, val x: Int, val y: Int)

private fun cantidadDeTiles(zoom: Int): Double = (1L shl zoom).toDouble()

/** Posición horizontal en "tiles" (con decimales) de una longitud, en el nivel de zoom dado. */
internal fun tileX(lng: Double, zoom: Int): Double = (lng + 180.0) / 360.0 * cantidadDeTiles(zoom)

/** Posición vertical en "tiles" (con decimales) de una latitud, en el nivel de zoom dado (Web Mercator). */
internal fun tileY(lat: Double, zoom: Int): Double {
    val radianes = lat.coerceIn(-LATITUD_MAXIMA, LATITUD_MAXIMA) * PI / 180.0
    return (1.0 - ln(tan(radianes) + 1.0 / cos(radianes)) / PI) / 2.0 * cantidadDeTiles(zoom)
}

internal fun lngDeTileX(x: Double, zoom: Int): Double = x / cantidadDeTiles(zoom) * 360.0 - 180.0

internal fun latDeTileY(y: Double, zoom: Int): Double {
    val n = PI - 2.0 * PI * y / cantidadDeTiles(zoom)
    return 180.0 / PI * atan(sinh(n))
}

/**
 * Los tiles que hacen falta para llenar una vista de [anchoPx] x [altoPx] centrada en (centroTileX, centroTileY),
 * ordenados del más cercano al centro al más lejano (así se descargan primero los que se ven mejor).
 * La longitud da la vuelta al mundo; la latitud no.
 */
internal fun clavesVisibles(
    centroTileX: Double,
    centroTileY: Double,
    zoom: Int,
    anchoPx: Int,
    altoPx: Int,
): List<ClaveTile> {
    if (anchoPx <= 0 || altoPx <= 0) return emptyList()

    val cantidad = 1 shl zoom
    val mitadX = anchoPx / 2.0 / TAMANO_TILE
    val mitadY = altoPx / 2.0 / TAMANO_TILE
    val desdeX = floor(centroTileX - mitadX).toInt()
    val hastaX = floor(centroTileX + mitadX).toInt()
    val desdeY = floor(centroTileY - mitadY).toInt().coerceAtLeast(0)
    val hastaY = floor(centroTileY + mitadY).toInt().coerceAtMost(cantidad - 1)

    val claves = mutableListOf<Pair<Double, ClaveTile>>()
    for (tx in desdeX..hastaX) {
        for (ty in desdeY..hastaY) {
            val distancia = (tx + 0.5 - centroTileX) * (tx + 0.5 - centroTileX) +
                (ty + 0.5 - centroTileY) * (ty + 0.5 - centroTileY)
            claves += distancia to ClaveTile(z = zoom, x = ((tx % cantidad) + cantidad) % cantidad, y = ty)
        }
    }
    return claves.sortedBy { it.first }.map { it.second }.distinct()
}

/** Dirección del tile en OpenTopoMap (la misma fuente que usa la app de Android). Reparte entre los servidores a, b y c. */
internal fun urlDeTileOpenTopo(clave: ClaveTile): String {
    val servidor = listOf("a", "b", "c")[(clave.x + clave.y) % 3]
    return "https://$servidor.tile.opentopomap.org/${clave.z}/${clave.x}/${clave.y}.png"
}
