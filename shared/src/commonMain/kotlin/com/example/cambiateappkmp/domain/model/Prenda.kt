package com.example.cambiateappkmp.domain.model

data class Prenda(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val categoria: Categoria,
    val colores: List<String>,
    val ocasiones: List<Ocasion>,
    val fotoUrl: String,
    /** Etiquetas invisibles que pone la IA (mismo campo que la app nativa). El usuario no las ve ni las edita. */
    val etiquetas: List<String> = emptyList(),
) {
    val esNueva: Boolean get() = id.isEmpty()
}