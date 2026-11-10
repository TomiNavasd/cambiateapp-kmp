package com.example.cambiateappkmp.presentation.wardrobe

import com.example.cambiateappkmp.domain.model.Categoria

fun Categoria.etiqueta(): String = when (this) {
    Categoria.SUPERIOR -> "Superior"
    Categoria.INFERIOR -> "Inferior"
    Categoria.CALZADO -> "Calzado"
    Categoria.ABRIGO -> "Abrigo"
    Categoria.ACCESORIO -> "Accesorio"
}