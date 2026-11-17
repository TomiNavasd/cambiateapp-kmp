package com.example.cambiateappkmp.presentation.outfits

import com.example.cambiateappkmp.domain.model.Ocasion

sealed interface OutfitsUiEvent {
    data class OcasionSeleccionada(val ocasion: Ocasion) : OutfitsUiEvent
    data object Sugerir : OutfitsUiEvent
    data class GuardarSugerencia(val indice: Int) : OutfitsUiEvent
    data class DescartarSugerencia(val indice: Int) : OutfitsUiEvent
    data object Reintentar : OutfitsUiEvent
    data object AvisoMostrado : OutfitsUiEvent
}
