package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.data.remote.OutfitDto
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.ocasionDeValorFirestore
import com.example.cambiateappkmp.domain.model.valorFirestore
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.time.Instant

/**
 * Los únicos campos que se escriben al actualizar un outfit existente (el mergeFields de la nativa).
 * El userId y la fecha no se tocan nunca.
 */
internal val CAMPOS_EDITABLES_OUTFIT = listOf("ocasion", "prendasIds", "favorito")

/** Null si el documento tiene una ocasión desconocida (dato corrupto). Sin fecha queda la época, como en la nativa. */
internal fun OutfitDto.aDomain(id: String): Outfit? {
    val ocasionDominio = ocasionDeValorFirestore(ocasion) ?: return null
    return Outfit(
        id = id,
        ocasion = ocasionDominio,
        prendasIds = prendasIds,
        fecha = fecha?.let { Instant.fromEpochSeconds(it.seconds, it.nanoseconds) } ?: Instant.fromEpochSeconds(0),
        favorito = favorito,
    )
}

internal fun Outfit.aDto(userId: String): OutfitDto = OutfitDto(
    userId = userId,
    ocasion = ocasion.valorFirestore,
    prendasIds = prendasIds,
    fecha = Timestamp(fecha.epochSeconds, fecha.nanosecondsOfSecond),
    favorito = favorito,
)

/** Del más nuevo al más viejo. Se ordena acá, en memoria, para no necesitar un índice en Firestore. */
internal fun List<Outfit>.ordenadosPorFecha(): List<Outfit> = sortedByDescending { it.fecha }
