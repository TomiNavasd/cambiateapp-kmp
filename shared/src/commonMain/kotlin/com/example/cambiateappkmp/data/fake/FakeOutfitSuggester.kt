package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.data.ai.ReglasOutfitSuggester
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.OutfitSuggester
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Sugeridor de ejemplo, sin red y sin clave: arma las combinaciones con las prendas del usuario de forma
 * repetible (siempre las mismas para las mismas prendas) y simula la demora de la IA.
 */
class FakeOutfitSuggester(private val demoraMs: Long = 600) : OutfitSuggester {

    override suspend fun sugerir(ocasion: Ocasion, prendas: List<Prenda>, clima: ClimaActual?): Result<List<Outfit>> {
        delay(demoraMs)
        return ReglasOutfitSuggester(Random(SEMILLA)).sugerir(ocasion, prendas, clima)
    }

    private companion object {
        const val SEMILLA = 7
    }
}
