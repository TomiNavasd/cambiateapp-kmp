package com.example.cambiateappkmp.presentation.outfits

import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit

sealed interface OutfitsUiEvent {
    data class OcasionSeleccionada(val ocasion: Ocasion) : OutfitsUiEvent
    data object Sugerir : OutfitsUiEvent
    data class GuardarSugerencia(val indice: Int) : OutfitsUiEvent
    data class DescartarSugerencia(val indice: Int) : OutfitsUiEvent
    data class AlternarFavorito(val outfit: Outfit) : OutfitsUiEvent
    data class Eliminar(val id: String) : OutfitsUiEvent
    /** El clima con el que se entró a la pantalla (desde el Mapa); null si se entró sin clima. */
    data class ClimaCambiado(val clima: ClimaActual?) : OutfitsUiEvent
    data object Reintentar : OutfitsUiEvent
    data object AvisoMostrado : OutfitsUiEvent
}
