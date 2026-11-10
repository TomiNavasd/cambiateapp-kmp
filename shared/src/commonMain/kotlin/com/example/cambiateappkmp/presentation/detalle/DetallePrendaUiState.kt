package com.example.cambiateappkmp.presentation.detalle

import com.example.cambiateappkmp.domain.model.Prenda

sealed interface DetallePrendaUiState {
    data object Cargando : DetallePrendaUiState
    data object NoEncontrada : DetallePrendaUiState
    data class Error(val mensaje: String) : DetallePrendaUiState
    data class Contenido(val prenda: Prenda) : DetallePrendaUiState
}