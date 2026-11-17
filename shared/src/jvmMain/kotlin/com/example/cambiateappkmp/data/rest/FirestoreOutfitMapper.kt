package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.mapper.CAMPOS_EDITABLES_OUTFIT
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.ocasionDeValorFirestore
import com.example.cambiateappkmp.domain.model.valorFirestore
import kotlin.time.Instant

/*
 * Conversión entre un Outfit y un documento de la colección "outfits" por la API REST de Firestore.
 * Los campos y los tipos son los mismos que usa la app nativa y el mapper de Android (GitLive):
 *   userId (string), ocasion (string en minúscula), prendasIds (array de string), fecha (timestamp), favorito (boolean).
 */

/** Todos los campos de un outfit, para crear el documento. El id no es un campo: es el nombre del documento. */
internal fun Outfit.aCamposFirestore(userId: String): Map<String, ValorFirestore> = mapOf(
    "userId" to ValorFirestore.Texto(userId),
    "ocasion" to ValorFirestore.Texto(ocasion.valorFirestore),
    "prendasIds" to ValorFirestore.Lista(prendasIds.map(ValorFirestore::Texto)),
    "fecha" to ValorFirestore.Fecha(fecha.toString()),
    "favorito" to ValorFirestore.Booleano(favorito),
)

/** Solo ocasion, prendasIds y favorito, para el PATCH con updateMask: nunca el userId ni la fecha. */
internal fun Outfit.aCamposEditablesFirestore(userId: String): Map<String, ValorFirestore> =
    aCamposFirestore(userId).filterKeys { it in CAMPOS_EDITABLES_OUTFIT }

/**
 * Lee un outfit de un documento. Null si la ocasión es desconocida (dato corrupto), como en la nativa.
 * Sin fecha, o con una fecha ilegible, queda la época (1970).
 */
internal fun DocumentoFirestore.aOutfit(): Outfit? {
    val ocasion = ocasionDeValorFirestore(texto("ocasion").orEmpty()) ?: return null
    return Outfit(
        id = id,
        ocasion = ocasion,
        prendasIds = listaDeTextos("prendasIds"),
        fecha = leerFecha((campos["fecha"] as? ValorFirestore.Fecha)?.valor),
        favorito = (campos["favorito"] as? ValorFirestore.Booleano)?.valor ?: false,
    )
}

private fun leerFecha(texto: String?): Instant =
    texto?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.fromEpochSeconds(0)
