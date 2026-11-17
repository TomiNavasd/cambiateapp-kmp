package com.example.cambiateappkmp.data.ai

import com.example.cambiateappkmp.domain.error.DomainException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** Lo que comparten los pedidos a Claude: el texto del primer bloque "text" de la respuesta. */
internal fun extraerTextoDeClaude(respuesta: String): String {
    val contenido = (Json.parseToJsonElement(respuesta) as? JsonObject)?.get("content") as? JsonArray
        ?: return ""
    for (bloque in contenido) {
        val objeto = bloque as? JsonObject ?: continue
        if ((objeto["type"] as? JsonPrimitive)?.contentOrNull == "text") {
            return (objeto["text"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        }
    }
    return ""
}

/** 401 y 403 son falta de permisos (clave inválida); cualquier otro código es un error desconocido. */
internal fun errorHttpDeClaude(codigo: Int): DomainException = when (codigo) {
    401, 403 -> DomainException.SinPermisos
    else -> DomainException.Desconocido(IllegalStateException("Claude respondió HTTP $codigo"))
}
