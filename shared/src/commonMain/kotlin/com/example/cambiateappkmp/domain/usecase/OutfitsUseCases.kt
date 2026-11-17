package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import kotlinx.coroutines.flow.Flow

class ObserveOutfitsUseCase(
    private val repository: OutfitsRepository,
) {
    operator fun invoke(): Flow<List<Outfit>> = repository.observarOutfits()
}

/** Borra el outfit; las prendas que lo formaban se mantienen en el armario. */
class DeleteOutfitUseCase(
    private val repository: OutfitsRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.eliminarOutfit(id)
}

/** Marca o desmarca como favorito un outfit ya guardado. Solo cambia ese campo. */
class AlternarFavoritoUseCase(
    private val repository: OutfitsRepository,
) {
    suspend operator fun invoke(outfit: Outfit): Result<Unit> =
        repository.cambiarFavorito(outfit.id, !outfit.favorito)
}
