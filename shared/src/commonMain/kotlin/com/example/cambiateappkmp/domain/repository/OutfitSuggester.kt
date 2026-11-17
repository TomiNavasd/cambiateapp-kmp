package com.example.cambiateappkmp.domain.repository

import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Prenda

/** Arma outfits sugeridos (todavía sin guardar: id vacío) con las prendas del usuario para una ocasión. */
interface OutfitSuggester {
    /** [clima] es opcional: si viene, el sugeridor puede tenerlo en cuenta. */
    suspend fun sugerir(ocasion: Ocasion, prendas: List<Prenda>, clima: ClimaActual?): Result<List<Outfit>>
}
