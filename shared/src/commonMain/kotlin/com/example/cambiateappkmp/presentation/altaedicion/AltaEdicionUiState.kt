package com.example.cambiateappkmp.presentation.altaedicion

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion

data class AltaEdicionUiState(
    val esEdicion: Boolean = false,
    val cargando: Boolean = false,
    val noEncontrada: Boolean = false,
    val errorCarga: String? = null,

    val nombre: String = "",
    val descripcion: String = "",
    val categoria: Categoria = Categoria.SUPERIOR,
    val colores: String = "",
    val ocasiones: Set<Ocasion> = emptySet(),
    val fotoUrl: String = "",

    val guardando: Boolean = false,
    val errorNombre: String? = null,
    val errorFoto: String? = null,
    val errorGeneral: String? = null,
)