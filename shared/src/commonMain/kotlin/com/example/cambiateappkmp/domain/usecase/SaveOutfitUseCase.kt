package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.error.CampoInvalido
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.repository.OutfitsRepository

/**
 * Guarda un outfit. No se puede guardar uno sin prendas ([DomainException.Validacion] con PRENDAS) ni uno que repita
 * la ocasión y el conjunto de prendas de otro ya guardado ([DomainException.OutfitYaGuardado]).
 */
class SaveOutfitUseCase(
    private val repository: OutfitsRepository,
) {
    suspend operator fun invoke(outfit: Outfit): Result<Unit> {
        if (outfit.prendasIds.isEmpty()) {
            return Result.failure(DomainException.Validacion(CampoInvalido.PRENDAS))
        }

        val guardados = repository.listarOutfits().getOrElse { return Result.failure(it) }
        val yaExiste = guardados.any { it.id != outfit.id && it.esIgualA(outfit) }
        if (yaExiste) return Result.failure(DomainException.OutfitYaGuardado)

        return repository.guardarOutfit(outfit)
    }

    private fun Outfit.esIgualA(otro: Outfit) =
        ocasion == otro.ocasion && prendasIds.toSet() == otro.prendasIds.toSet()
}
