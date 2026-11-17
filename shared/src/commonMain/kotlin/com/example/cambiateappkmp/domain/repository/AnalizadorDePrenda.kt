package com.example.cambiateappkmp.domain.repository

import com.example.cambiateappkmp.domain.model.AnalisisPrenda

interface AnalizadorDePrenda {
    /** Analiza la foto (por su URL pública) y devuelve nombre, descripción, categoría, colores, ocasiones y etiquetas. */
    suspend fun analizar(fotoUrl: String, idioma: String = "es"): Result<AnalisisPrenda>
}
