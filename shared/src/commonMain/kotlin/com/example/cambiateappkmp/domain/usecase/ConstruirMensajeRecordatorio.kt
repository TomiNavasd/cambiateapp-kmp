package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.model.PronosticoManiana
import kotlin.math.roundToInt

private const val UMBRAL_LLUVIA_PCT = 50
private const val UMBRAL_FRIO_C = 12.0
private const val UMBRAL_CALOR_C = 26.0

/**
 * Arma el título y el texto del recordatorio según el pronóstico de mañana.
 * Las reglas van por prioridad: primero lluvia, después frío, después calor.
 */
fun construirMensajeRecordatorio(pronostico: PronosticoManiana): Pair<String, String> {
    val minima = pronostico.minimaC.roundToInt()
    val maxima = pronostico.maximaC.roundToInt()

    return when {
        pronostico.probabilidadLluvia >= UMBRAL_LLUVIA_PCT ->
            "Mañana llueve" to
                "Hay ${pronostico.probabilidadLluvia}% de lluvia ($minima° / $maxima°): llevá paraguas o una campera impermeable."
        pronostico.minimaC < UMBRAL_FRIO_C ->
            "Mañana hace frío" to "La mínima será de $minima°: abrigate bien."
        pronostico.maximaC >= UMBRAL_CALOR_C ->
            "Mañana hace calor" to "La máxima será de $maxima°: elegí ropa liviana."
        else ->
            "Mañana, clima agradable" to
                "Entre $minima° y $maxima°: no necesitás abrigo especial. ¡Elegí el outfit que más te guste!"
    }
}

/** Aviso que sale cuando no se pudo consultar el clima: el recordatorio nunca queda en silencio. */
val MENSAJE_RECORDATORIO_GENERAL: Pair<String, String> =
    "Mirá el clima de mañana" to "Abrí CambiateApp y fijate qué ponerte según el pronóstico."
