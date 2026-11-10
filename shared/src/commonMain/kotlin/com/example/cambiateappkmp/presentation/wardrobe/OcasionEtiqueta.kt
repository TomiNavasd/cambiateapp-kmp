package com.example.cambiateappkmp.presentation.wardrobe

import com.example.cambiateappkmp.domain.model.Ocasion

fun Ocasion.etiqueta(): String = when (this) {
    Ocasion.TRABAJO -> "Trabajo"
    Ocasion.FACULTAD -> "Facultad"
    Ocasion.SALIDA -> "Salida"
    Ocasion.CASAMIENTO -> "Casamiento"
    Ocasion.DEPORTE -> "Deporte"
    Ocasion.CASUAL -> "Casual"
}