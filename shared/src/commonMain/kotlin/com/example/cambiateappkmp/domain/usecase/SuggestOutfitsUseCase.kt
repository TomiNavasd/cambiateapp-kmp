package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.repository.OutfitSuggester
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first

/** Pide sugerencias de outfits para una ocasión con las prendas actuales del usuario. */
class SuggestOutfitsUseCase(
    private val observePrendas: ObservePrendasUseCase,
    private val suggester: OutfitSuggester,
) {
    suspend operator fun invoke(ocasion: Ocasion, clima: ClimaActual? = null): Result<List<Outfit>> {
        val prendas = try {
            observePrendas().first()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return suggester.sugerir(ocasion, prendas, clima)
    }
}
