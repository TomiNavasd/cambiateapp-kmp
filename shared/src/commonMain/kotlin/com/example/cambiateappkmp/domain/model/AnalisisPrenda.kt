package com.example.cambiateappkmp.domain.model

/** Lo que la IA entendió de la foto de una prenda. Todo es una sugerencia: el usuario lo puede corregir. */
data class AnalisisPrenda(
    val nombre: String,
    val descripcion: String,
    val categoria: Categoria?,
    val colores: List<String>,
    val ocasiones: List<Ocasion>,
    val etiquetas: List<String>,
)
