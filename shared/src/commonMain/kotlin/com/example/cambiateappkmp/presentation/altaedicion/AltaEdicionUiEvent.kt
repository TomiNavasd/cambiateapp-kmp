package com.example.cambiateappkmp.presentation.altaedicion

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion

sealed interface AltaEdicionUiEvent {
    data class NombreCambio(val valor: String) : AltaEdicionUiEvent
    data class DescripcionCambio(val valor: String) : AltaEdicionUiEvent
    data class CategoriaSeleccionada(val categoria: Categoria) : AltaEdicionUiEvent
    data class ColoresCambio(val valor: String) : AltaEdicionUiEvent
    data class OcasionAlternada(val ocasion: Ocasion) : AltaEdicionUiEvent
    data class FotoSubida(val url: String) : AltaEdicionUiEvent
    data object Guardar : AltaEdicionUiEvent
    data object ReintentarCarga : AltaEdicionUiEvent
}