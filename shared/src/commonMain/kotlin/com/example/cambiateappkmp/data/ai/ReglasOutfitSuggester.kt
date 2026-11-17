package com.example.cambiateappkmp.data.ai

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.OutfitSuggester
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Respaldo sin IA: combina al azar superior + inferior + calzado de las prendas de la ocasión, y le suma un abrigo
 * y un accesorio rotativos si hay. Es la misma lógica que la app nativa; no usa el clima.
 */
class ReglasOutfitSuggester(
    private val random: Random = Random.Default,
    private val ahora: () -> Instant = { Clock.System.now() },
) : OutfitSuggester {

    override suspend fun sugerir(ocasion: Ocasion, prendas: List<Prenda>, clima: ClimaActual?): Result<List<Outfit>> {
        val porCategoria = prendas
            .filter { ocasion in it.ocasiones }
            .groupBy { it.categoria }

        fun elegir(categoria: Categoria): List<Prenda> =
            porCategoria[categoria].orEmpty().shuffled(random).take(MAX_POR_CATEGORIA)

        val superiores = elegir(Categoria.SUPERIOR)
        val inferiores = elegir(Categoria.INFERIOR)
        val calzados = elegir(Categoria.CALZADO)
        if (superiores.isEmpty() || inferiores.isEmpty() || calzados.isEmpty()) {
            return Result.success(emptyList())
        }

        val abrigos = elegir(Categoria.ABRIGO)
        val accesorios = elegir(Categoria.ACCESORIO)

        val combinaciones = buildList {
            for (superior in superiores) {
                for (inferior in inferiores) {
                    for (calzado in calzados) add(Triple(superior, inferior, calzado))
                }
            }
        }.shuffled(random).take(MAX_OUTFITS)

        val fecha = ahora()
        val outfits = combinaciones.mapIndexed { indice, (superior, inferior, calzado) ->
            val prendasDelOutfit = buildList {
                add(superior)
                add(inferior)
                add(calzado)
                if (abrigos.isNotEmpty()) add(abrigos[indice % abrigos.size])
                if (accesorios.isNotEmpty()) add(accesorios[indice % accesorios.size])
            }
            Outfit(
                id = "",
                ocasion = ocasion,
                prendasIds = prendasDelOutfit.map { it.id },
                fecha = fecha,
                favorito = false,
            )
        }
        return Result.success(outfits)
    }

    private companion object {
        const val MAX_OUTFITS = 10
        const val MAX_POR_CATEGORIA = 4
    }
}
