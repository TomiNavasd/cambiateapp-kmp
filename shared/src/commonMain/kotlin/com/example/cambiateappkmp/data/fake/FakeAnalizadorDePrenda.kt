package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.domain.model.AnalisisPrenda
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.repository.AnalizadorDePrenda
import kotlinx.coroutines.delay

/** Analizador de ejemplo, sin red y sin clave: siempre "entiende" lo mismo y simula la demora de la IA. */
class FakeAnalizadorDePrenda(private val demoraMs: Long = 800) : AnalizadorDePrenda {

    override suspend fun analizar(fotoUrl: String, idioma: String): Result<AnalisisPrenda> {
        delay(demoraMs)
        return Result.success(
            AnalisisPrenda(
                nombre = "Jean azul recto",
                descripcion = "Jean de corte recto, tiro alto",
                categoria = Categoria.INFERIOR,
                colores = listOf("azul"),
                ocasiones = listOf(Ocasion.CASUAL, Ocasion.FACULTAD),
                etiquetas = listOf("casual", "jean", "recto", "diario", "versatil"),
            ),
        )
    }
}
