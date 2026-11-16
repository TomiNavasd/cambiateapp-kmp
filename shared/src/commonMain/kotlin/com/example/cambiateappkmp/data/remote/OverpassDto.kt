package com.example.cambiateappkmp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Forma de la respuesta de Overpass (OpenStreetMap), solo con los campos que usamos. No necesita clave. */
@Serializable
data class OverpassRespuesta(
    val elements: List<OverpassElemento> = emptyList(),
)

@Serializable
data class OverpassElemento(
    val type: String? = null,
    val id: Long = 0,
    val lat: Double? = null,
    @SerialName("lon") val lng: Double? = null,
    val center: OverpassCentro? = null,
    val tags: Map<String, String>? = null,
)

/** Los locales que son un edificio (way) no traen lat/lon propios: vienen en el centro. */
@Serializable
data class OverpassCentro(
    val lat: Double,
    @SerialName("lon") val lng: Double,
)
