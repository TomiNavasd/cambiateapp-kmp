package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal const val MENSAJE_OUTFITS_NO_DISPONIBLE = "No disponible todavía en escritorio"

/**
 * Versión mínima de escritorio: todavía no hay outfits por la API REST de Firestore (queda pendiente).
 * No hay outfits guardados que mostrar y cualquier operación devuelve un error claro.
 */
internal class OutfitsRepositoryJvm : OutfitsRepository {

    override fun observarOutfits(): Flow<List<Outfit>> = flowOf(emptyList())

    override suspend fun listarOutfits(): Result<List<Outfit>> = noDisponible()

    override suspend fun guardarOutfit(outfit: Outfit): Result<Unit> = noDisponible()

    override suspend fun eliminarOutfit(id: String): Result<Unit> = noDisponible()

    override suspend fun cambiarFavorito(id: String, favorito: Boolean): Result<Unit> = noDisponible()

    private fun <T> noDisponible(): Result<T> =
        Result.failure(DomainException.Desconocido(UnsupportedOperationException(MENSAJE_OUTFITS_NO_DISPONIBLE)))
}
