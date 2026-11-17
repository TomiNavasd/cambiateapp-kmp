package com.example.cambiateappkmp.data.ai

import com.example.cambiateappkmp.domain.model.AnalisisPrenda
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal object RespuestaAnalisisParser {

    /** Lee el texto de Claude: lo que está entre la primera `{` y la última `}`. Null si no se puede leer como JSON. */
    fun parsear(texto: String): AnalisisPrenda? {
        val inicio = texto.indexOf('{')
        val fin = texto.lastIndexOf('}')
        if (inicio !in 0 until fin) return null

        val json = try {
            Json.parseToJsonElement(texto.substring(inicio, fin + 1)) as? JsonObject
        } catch (e: Exception) {
            null
        } ?: return null

        return AnalisisPrenda(
            nombre = json["nombre"].comoTexto(),
            descripcion = json["descripcion"].comoTexto(),
            categoria = Categoria.entries.firstOrNull { it.name.equals(json["categoria"].comoTexto().trim(), ignoreCase = true) },
            colores = json["colores"].comoListaDeTextos(),
            ocasiones = json["ocasiones"].comoListaDeTextos().mapNotNull { valor ->
                Ocasion.entries.firstOrNull { it.name.equals(valor.trim(), ignoreCase = true) }
            },
            etiquetas = json["etiquetas"].comoListaDeTextos(),
        )
    }

    private fun JsonElement?.comoTexto(): String = (this as? JsonPrimitive)?.contentOrNull.orEmpty()

    private fun JsonElement?.comoListaDeTextos(): List<String> =
        (this as? JsonArray)?.map { it.comoTexto() }?.filter { it.isNotBlank() }.orEmpty()
}
