package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Outfits en memoria, sin Firebase. Los ejemplos usan los ids de las prendas de [FakePrendaRepository] ("1" a "10").
 * Se puede pasar otra lista inicial (por ejemplo, vacía) para los tests.
 */
class FakeOutfitsRepository(iniciales: List<Outfit> = outfitsDeEjemplo()) : OutfitsRepository {

    private val outfits = MutableStateFlow(iniciales)
    private var contadorIds = 100

    override fun observarOutfits(): Flow<List<Outfit>> = outfits.map { it.ordenados() }

    override suspend fun listarOutfits(): Result<List<Outfit>> = Result.success(outfits.value.ordenados())

    override suspend fun guardarOutfit(outfit: Outfit): Result<Unit> {
        if (outfit.esNuevo) {
            contadorIds += 1
            outfits.update { it + outfit.copy(id = "local-$contadorIds") }
        } else {
            // Como en Firestore: solo cambian la ocasión, las prendas y el favorito.
            outfits.update { lista ->
                lista.map {
                    if (it.id == outfit.id) {
                        it.copy(ocasion = outfit.ocasion, prendasIds = outfit.prendasIds, favorito = outfit.favorito)
                    } else {
                        it
                    }
                }
            }
        }
        return Result.success(Unit)
    }

    override suspend fun eliminarOutfit(id: String): Result<Unit> {
        outfits.update { lista -> lista.filterNot { it.id == id } }
        return Result.success(Unit)
    }

    override suspend fun cambiarFavorito(id: String, favorito: Boolean): Result<Unit> {
        if (outfits.value.none { it.id == id }) {
            return Result.failure(DomainException.Desconocido(IllegalArgumentException("No existe el outfit $id")))
        }
        outfits.update { lista -> lista.map { if (it.id == id) it.copy(favorito = favorito) else it } }
        return Result.success(Unit)
    }

    private fun List<Outfit>.ordenados() = sortedByDescending { it.fecha }
}

private fun outfitsDeEjemplo(): List<Outfit> {
    val ahora = Clock.System.now()
    return listOf(
        Outfit("o1", Ocasion.CASUAL, listOf("1", "4", "6"), ahora - 1.days, favorito = true),
        Outfit("o2", Ocasion.TRABAJO, listOf("2", "5", "7", "9"), ahora - 3.days, favorito = false),
        Outfit("o3", Ocasion.SALIDA, listOf("3", "4", "6", "8", "10"), ahora - 7.days, favorito = false),
    )
}
