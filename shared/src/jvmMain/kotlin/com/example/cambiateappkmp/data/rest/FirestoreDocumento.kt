package com.example.cambiateappkmp.data.rest

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** Un valor de Firestore con su tipo, tal como lo maneja la API REST (stringValue, integerValue, etc.). */
internal sealed interface ValorFirestore {
    data class Texto(val valor: String) : ValorFirestore
    data class Entero(val valor: Long) : ValorFirestore
    data class Decimal(val valor: Double) : ValorFirestore
    data class Booleano(val valor: Boolean) : ValorFirestore

    /** Fecha y hora en formato RFC 3339, por ejemplo 2026-11-16T15:45:00Z. */
    data class Fecha(val valor: String) : ValorFirestore

    data class Lista(val valores: List<ValorFirestore>) : ValorFirestore
    data class Mapa(val campos: Map<String, ValorFirestore>) : ValorFirestore
    data object Nulo : ValorFirestore
}

internal fun ValorFirestore.aJson(): JsonObject = when (this) {
    is ValorFirestore.Texto -> JsonObject(mapOf("stringValue" to JsonPrimitive(valor)))
    // La API manda los enteros como texto para no perder precisión.
    is ValorFirestore.Entero -> JsonObject(mapOf("integerValue" to JsonPrimitive(valor.toString())))
    is ValorFirestore.Decimal -> JsonObject(mapOf("doubleValue" to JsonPrimitive(valor)))
    is ValorFirestore.Booleano -> JsonObject(mapOf("booleanValue" to JsonPrimitive(valor)))
    is ValorFirestore.Fecha -> JsonObject(mapOf("timestampValue" to JsonPrimitive(valor)))
    is ValorFirestore.Lista -> JsonObject(
        mapOf(
            "arrayValue" to if (valores.isEmpty()) {
                JsonObject(emptyMap())
            } else {
                JsonObject(mapOf("values" to JsonArray(valores.map { it.aJson() })))
            },
        ),
    )
    is ValorFirestore.Mapa -> JsonObject(
        mapOf(
            "mapValue" to JsonObject(
                mapOf("fields" to JsonObject(campos.mapValues { (_, valor) -> valor.aJson() })),
            ),
        ),
    )
    ValorFirestore.Nulo -> JsonObject(mapOf("nullValue" to JsonNull))
}

/** Lee un valor de Firestore. Los tipos que la app no usa (geopuntos, referencias, bytes) quedan como Nulo. */
internal fun JsonElement.aValorFirestore(): ValorFirestore {
    val objeto = this as? JsonObject ?: return ValorFirestore.Nulo
    objeto["stringValue"]?.let { return ValorFirestore.Texto(it.contenido().orEmpty()) }
    objeto["integerValue"]?.let { return it.contenido()?.toLongOrNull()?.let(ValorFirestore::Entero) ?: ValorFirestore.Nulo }
    objeto["doubleValue"]?.let { return it.contenido()?.toDoubleOrNull()?.let(ValorFirestore::Decimal) ?: ValorFirestore.Nulo }
    objeto["booleanValue"]?.let { return ((it as? JsonPrimitive)?.booleanOrNull)?.let(ValorFirestore::Booleano) ?: ValorFirestore.Nulo }
    objeto["timestampValue"]?.let { return ValorFirestore.Fecha(it.contenido().orEmpty()) }
    objeto["arrayValue"]?.let { lista ->
        val valores = (lista as? JsonObject)?.get("values")?.jsonArray.orEmpty()
        return ValorFirestore.Lista(valores.map { it.aValorFirestore() })
    }
    objeto["mapValue"]?.let { mapa ->
        val campos = (mapa as? JsonObject)?.get("fields")?.jsonObject.orEmpty()
        return ValorFirestore.Mapa(campos.mapValues { (_, valor) -> valor.aValorFirestore() })
    }
    return ValorFirestore.Nulo
}

private fun JsonElement.contenido(): String? = (this as? JsonPrimitive)?.contentOrNull

/** Un documento de Firestore: su ruta completa (si ya existe) y sus campos. */
internal data class DocumentoFirestore(
    /** Ruta completa, por ejemplo projects/p/databases/(default)/documents/prendas/abc123. Es null al crear. */
    val nombre: String?,
    val campos: Map<String, ValorFirestore>,
) {
    /** El id es el último tramo de la ruta. */
    val id: String get() = nombre.orEmpty().substringAfterLast('/')

    /** El cuerpo que pide la API para crear o modificar: {"fields": {...}}. */
    fun aJsonCuerpo(): JsonObject =
        JsonObject(mapOf("fields" to JsonObject(campos.mapValues { (_, valor) -> valor.aJson() })))

    fun texto(campo: String): String? = (campos[campo] as? ValorFirestore.Texto)?.valor

    fun listaDeTextos(campo: String): List<String> =
        ((campos[campo] as? ValorFirestore.Lista)?.valores.orEmpty()).mapNotNull { (it as? ValorFirestore.Texto)?.valor }

    companion object {
        fun desdeJson(elemento: JsonElement): DocumentoFirestore {
            val objeto = elemento.jsonObject
            val campos = objeto["fields"]?.jsonObject.orEmpty()
            return DocumentoFirestore(
                nombre = objeto["name"].let { (it as? JsonPrimitive)?.contentOrNull },
                campos = campos.mapValues { (_, valor) -> valor.aValorFirestore() },
            )
        }
    }
}
