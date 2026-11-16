package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.data.remote.OpenMeteoRespuesta
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.PronosticoManiana

/** En las listas diarias de la API, la posición 0 es hoy y la 1 es mañana. */
private const val INDICE_MANIANA = 1

/** Convierte el código de clima de la OMM (WMO) en una descripción en español. */
internal fun descripcionClima(codigo: Int?): String = when (codigo) {
    0 -> "Despejado"
    1 -> "Mayormente despejado"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    51, 53, 55 -> "Llovizna"
    56, 57 -> "Llovizna helada"
    61, 63, 65 -> "Lluvia"
    66, 67 -> "Lluvia helada"
    71, 73, 75 -> "Nieve"
    77 -> "Granizo fino"
    80, 81, 82 -> "Chubascos"
    85, 86 -> "Chubascos de nieve"
    95 -> "Tormenta"
    96, 99 -> "Tormenta con granizo"
    else -> "Sin datos"
}

/** Llovizna, lluvia, chubascos y tormentas cuentan como lluvia. La nieve no. */
internal fun llueveSegunCodigo(codigo: Int?): Boolean =
    codigo != null && (codigo in 51..67 || codigo in 80..82 || codigo in 95..99)

internal fun OpenMeteoRespuesta.aClimaActual(): ClimaActual? {
    val actual = current ?: return null
    val temperatura = actual.temperatura ?: return null
    val llueve = llueveSegunCodigo(actual.codigoClima) || (actual.precipitation ?: 0.0) > 0.0
    return ClimaActual(
        temperaturaC = temperatura,
        descripcion = descripcionClima(actual.codigoClima),
        llueve = llueve,
    )
}

internal fun OpenMeteoRespuesta.aPronosticoManiana(): PronosticoManiana? {
    val diario = daily ?: return null
    val maxima = diario.maxima?.getOrNull(INDICE_MANIANA) ?: return null
    val minima = diario.minima?.getOrNull(INDICE_MANIANA) ?: return null
    // La probabilidad de lluvia puede venir null; en ese caso asumimos 0 %.
    val probabilidad = (diario.probabilidadLluvia?.getOrNull(INDICE_MANIANA) ?: 0).coerceIn(0, 100)
    return PronosticoManiana(
        maximaC = maxima,
        minimaC = minima,
        probabilidadLluvia = probabilidad,
        descripcion = descripcionClima(diario.codigoClima?.getOrNull(INDICE_MANIANA)),
    )
}
