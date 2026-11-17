package com.example.cambiateappkmp.data.ai

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal object RespuestaClaudeParser {

    private const val MAX_OUTFITS = 10

    /**
     * Lee el texto de Claude: toma lo que está entre la primera `{` y la última `}`, descarta los ids que no
     * mandamos, exige superior + inferior + calzado, descarta repetidos y se queda con 10 como máximo.
     */
    fun parsear(
        texto: String,
        ocasion: Ocasion,
        categoriaPorId: Map<String, Categoria>,
        ahora: Instant,
    ): List<Outfit> {
        val json = extraerJson(texto) ?: return emptyList()
        val outfitsJson = try {
            (Json.parseToJsonElement(json) as? JsonObject)?.get("outfits") as? JsonArray
        } catch (e: Exception) {
            null
        } ?: return emptyList()

        val vistos = mutableSetOf<Set<String>>()
        val resultado = mutableListOf<Outfit>()

        for (elemento in outfitsJson) {
            val ids = ((elemento as? JsonObject)?.get("prendasIds") as? JsonArray)
                ?.map { (it as? JsonPrimitive)?.contentOrNull.orEmpty() }
                .orEmpty()
                .filter { it in categoriaPorId } // descarta ids inventados
                .distinct()

            val categorias = ids.mapNotNull { categoriaPorId[it] }.toSet()
            val completo = Categoria.SUPERIOR in categorias &&
                Categoria.INFERIOR in categorias &&
                Categoria.CALZADO in categorias

            if (!completo || !vistos.add(ids.toSet())) continue

            resultado += Outfit(
                id = "",
                ocasion = ocasion,
                prendasIds = ids,
                fecha = ahora,
                favorito = false,
            )
            if (resultado.size == MAX_OUTFITS) break
        }
        return resultado
    }

    private fun extraerJson(texto: String): String? {
        val inicio = texto.indexOf('{')
        val fin = texto.lastIndexOf('}')
        return if (inicio in 0 until fin) texto.substring(inicio, fin + 1) else null
    }
}
