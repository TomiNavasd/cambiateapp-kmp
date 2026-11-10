package com.example.cambiateappkmp.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PrendaDto(
    val userId: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val categoria: String = "",
    val colores: List<String> = emptyList(),
    val ocasiones: List<String> = emptyList(),
    val fotoUrl: String = "",
)