package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.data.remote.OverpassElemento
import com.example.cambiateappkmp.data.remote.OverpassRespuesta
import com.example.cambiateappkmp.domain.model.LocalRopa
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

private const val MAXIMO_LOCALES = 30
private const val RADIO_TIERRA_METROS = 6_371_000.0

/** Consulta de Overpass: tiendas de ropa (shop=clothes), como punto o como edificio, dentro del radio. */
internal fun consultaOverpass(lat: Double, lng: Double, radioMetros: Int): String =
    "[out:json][timeout:25];(" +
        "node[\"shop\"=\"clothes\"](around:$radioMetros,$lat,$lng);" +
        "way[\"shop\"=\"clothes\"](around:$radioMetros,$lat,$lng);" +
        ");out center tags;"

/** Pasa la respuesta a locales, los ordena del más cercano al más lejano y se queda con los primeros. */
internal fun OverpassRespuesta.aLocales(origenLat: Double, origenLng: Double): List<LocalRopa> =
    elements
        .mapNotNull { it.aLocalRopa() }
        .sortedBy { distanciaMetros(origenLat, origenLng, it.lat, it.lng) }
        .take(MAXIMO_LOCALES)

private fun OverpassElemento.aLocalRopa(): LocalRopa? {
    val latitud = lat ?: center?.lat ?: return null
    val longitud = lng ?: center?.lng ?: return null
    val nombre = tags?.get("name")?.takeIf { it.isNotBlank() } ?: "Tienda de ropa"
    return LocalRopa(
        id = "${type ?: "elemento"}/$id",
        nombre = nombre,
        direccion = direccionDe(tags),
        lat = latitud,
        lng = longitud,
    )
}

/** Arma "Calle 123, Ciudad" con lo que haya; si no hay nada, queda vacío. */
internal fun direccionDe(tags: Map<String, String>?): String {
    val calle = tags?.get("addr:street")?.takeIf { it.isNotBlank() }
    val numero = tags?.get("addr:housenumber")?.takeIf { it.isNotBlank() }
    val ciudad = tags?.get("addr:city")?.takeIf { it.isNotBlank() }
    val lineaCalle = when {
        calle != null && numero != null -> "$calle $numero"
        else -> calle
    }
    return listOfNotNull(lineaCalle, ciudad).joinToString(", ")
}

/** Distancia aproximada en metros. Alcanza para ordenar locales a pocos kilómetros. */
internal fun distanciaMetros(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val aRadianes = PI / 180.0
    val dLat = (lat2 - lat1) * aRadianes
    val dLng = (lng2 - lng1) * aRadianes * cos((lat1 + lat2) / 2.0 * aRadianes)
    return RADIO_TIERRA_METROS * sqrt(dLat * dLat + dLng * dLng)
}
