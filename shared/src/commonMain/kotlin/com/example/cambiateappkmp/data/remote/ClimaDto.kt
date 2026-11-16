package com.example.cambiateappkmp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Forma de la respuesta de Open-Meteo (solo los campos que usamos). No necesita clave de API. */
@Serializable
data class OpenMeteoRespuesta(
    val current: ClimaActualDto? = null,
    val daily: ClimaDiarioDto? = null,
)

@Serializable
data class ClimaActualDto(
    @SerialName("temperature_2m") val temperatura: Double? = null,
    @SerialName("weather_code") val codigoClima: Int? = null,
    val precipitation: Double? = null,
)

@Serializable
data class ClimaDiarioDto(
    @SerialName("temperature_2m_max") val maxima: List<Double?>? = null,
    @SerialName("temperature_2m_min") val minima: List<Double?>? = null,
    @SerialName("precipitation_probability_max") val probabilidadLluvia: List<Int?>? = null,
    @SerialName("weather_code") val codigoClima: List<Int?>? = null,
)
