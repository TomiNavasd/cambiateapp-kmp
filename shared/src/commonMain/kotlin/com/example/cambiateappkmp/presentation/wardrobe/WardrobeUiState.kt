package com.example.cambiateappkmp.presentation.wardrobe

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Prenda

sealed interface WardrobeUiState {
    data object Cargando : WardrobeUiState
    data object Vacio : WardrobeUiState
    data class Error(val mensaje: String) : WardrobeUiState
    data class Contenido(
        val prendas: List<Prenda>,
        val filtro: Categoria?,
    ) : WardrobeUiState
}