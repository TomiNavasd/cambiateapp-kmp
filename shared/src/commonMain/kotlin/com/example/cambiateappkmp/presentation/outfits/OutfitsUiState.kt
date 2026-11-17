package com.example.cambiateappkmp.presentation.outfits

import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Prenda

/** Un outfit con las prendas completas (las que ya no existen en el armario no aparecen). */
data class OutfitConPrendas(val outfit: Outfit, val prendas: List<Prenda>)

sealed interface OutfitsUiState {
    data object Cargando : OutfitsUiState
    data class Error(val mensaje: String) : OutfitsUiState
    data class Contenido(
        val ocasion: Ocasion,
        val sugerencias: List<OutfitConPrendas>,
        val guardados: List<OutfitConPrendas>,
        val sugiriendo: Boolean,
        /** Mensaje de una sola vez (se muestra en un Snackbar y se limpia con AvisoMostrado). */
        val aviso: String?,
    ) : OutfitsUiState
}
