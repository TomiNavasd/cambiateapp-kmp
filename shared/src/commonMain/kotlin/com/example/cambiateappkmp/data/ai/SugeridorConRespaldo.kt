package com.example.cambiateappkmp.data.ai

import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.OutfitSuggester

/**
 * Usa el sugeridor principal (Claude); si falla o devuelve una lista vacía, usa el de respaldo (las reglas simples).
 * [alFallar] recibe el error del principal, por si hay que dejar registro.
 */
class SugeridorConRespaldo(
    private val principal: OutfitSuggester,
    private val respaldo: OutfitSuggester,
    private val alFallar: (Throwable) -> Unit = {},
) : OutfitSuggester {

    override suspend fun sugerir(ocasion: Ocasion, prendas: List<Prenda>, clima: ClimaActual?): Result<List<Outfit>> {
        val delPrincipal = principal.sugerir(ocasion, prendas, clima)
        if (!delPrincipal.getOrNull().isNullOrEmpty()) return delPrincipal

        delPrincipal.exceptionOrNull()?.let(alFallar)

        return respaldo.sugerir(ocasion, prendas, clima)
    }
}
