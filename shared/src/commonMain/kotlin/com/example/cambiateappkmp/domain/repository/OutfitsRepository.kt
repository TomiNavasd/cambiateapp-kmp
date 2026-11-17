package com.example.cambiateappkmp.domain.repository

import com.example.cambiateappkmp.domain.model.Outfit
import kotlinx.coroutines.flow.Flow

interface OutfitsRepository {
    /** Los outfits del usuario, del más nuevo al más viejo. */
    fun observarOutfits(): Flow<List<Outfit>>

    /** Lo mismo, una sola vez. */
    suspend fun listarOutfits(): Result<List<Outfit>>

    /** Outfit nuevo (sin id): lo crea. Con id: actualiza solo ocasión, prendas y favorito. */
    suspend fun guardarOutfit(outfit: Outfit): Result<Unit>

    suspend fun eliminarOutfit(id: String): Result<Unit>

    /** Cambia solamente el campo favorito. */
    suspend fun cambiarFavorito(id: String, favorito: Boolean): Result<Unit>
}
