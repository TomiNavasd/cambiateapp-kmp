package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.AnalisisPrenda
import com.example.cambiateappkmp.domain.model.VocabularioEtiquetas
import com.example.cambiateappkmp.domain.repository.AnalizadorDePrenda
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pide el análisis de la foto de una prenda y lo deja limpio: recorta los textos, normaliza los colores y se queda
 * solo con las etiquetas del vocabulario cerrado (como mucho 12). Tarda 20 segundos como máximo.
 */
class AnalizarPrendaUseCase(
    private val analizador: AnalizadorDePrenda,
) {
    suspend operator fun invoke(fotoUrl: String, idioma: String = "es"): Result<AnalisisPrenda> {
        if (fotoUrl.isBlank()) {
            return Result.failure(DomainException.Desconocido(IllegalArgumentException("Falta la foto")))
        }

        val resultado = withTimeoutOrNull(TIEMPO_MAXIMO_MS) { analizador.analizar(fotoUrl, idioma) }
            ?: return Result.failure(DomainException.Desconocido(IllegalStateException("El análisis tardó demasiado")))

        return resultado.map(::limpiar)
    }

    private fun limpiar(cruda: AnalisisPrenda) = AnalisisPrenda(
        nombre = cruda.nombre.trim().take(MAX_NOMBRE),
        descripcion = cruda.descripcion.trim().take(MAX_DESCRIPCION),
        categoria = cruda.categoria,
        colores = cruda.colores
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_COLORES),
        ocasiones = cruda.ocasiones.distinct(),
        etiquetas = cruda.etiquetas
            .map { it.trim().lowercase() }
            .filter { it in VocabularioEtiquetas.PERMITIDAS } // descarta lo que la IA invente
            .distinct()
            .take(VocabularioEtiquetas.MAXIMO_POR_PRENDA),
    )

    private companion object {
        const val TIEMPO_MAXIMO_MS = 20_000L
        const val MAX_NOMBRE = 40
        const val MAX_DESCRIPCION = 120
        const val MAX_COLORES = 4
    }
}

/** Solo las etiquetas de una foto: se usa al guardar una prenda que todavía no las tiene. Si falla, devuelve vacío. */
class EtiquetarPrendaUseCase(
    private val analizarPrenda: AnalizarPrendaUseCase,
) {
    suspend operator fun invoke(fotoUrl: String): List<String> =
        analizarPrenda(fotoUrl).getOrNull()?.etiquetas.orEmpty()
}
