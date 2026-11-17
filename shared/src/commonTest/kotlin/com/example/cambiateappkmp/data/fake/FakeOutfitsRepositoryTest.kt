package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class FakeOutfitsRepositoryTest {

    private val ahora = Instant.fromEpochSeconds(1_763_000_000)

    @Test
    fun losEjemplosUsanPrendasDelFakeYVienenOrdenados() = runTest {
        val outfits = FakeOutfitsRepository().observarOutfits().first()
        val idsDePrendasFake = (1..10).map { it.toString() }.toSet()

        assertTrue(outfits.isNotEmpty())
        assertTrue(outfits.all { o -> o.prendasIds.all { it in idsDePrendasFake } })
        assertEquals(outfits.sortedByDescending { it.fecha }, outfits)
    }

    @Test
    fun guardarUnoNuevoLeAsignaIdYAparece() = runTest {
        val repo = FakeOutfitsRepository(emptyList())

        repo.guardarOutfit(Outfit("", Ocasion.CASUAL, listOf("1", "4", "6"), ahora, favorito = false))

        val guardado = repo.listarOutfits().getOrThrow().single()
        assertTrue(guardado.id.isNotEmpty())
        assertEquals(Ocasion.CASUAL, guardado.ocasion)
    }

    @Test
    fun actualizarCambiaOcasionPrendasYFavoritoPeroNoLaFecha() = runTest {
        val original = Outfit("o1", Ocasion.CASUAL, listOf("1"), ahora, favorito = false)
        val repo = FakeOutfitsRepository(listOf(original))

        repo.guardarOutfit(original.copy(ocasion = Ocasion.TRABAJO, prendasIds = listOf("2", "5"), favorito = true, fecha = Instant.fromEpochSeconds(5)))

        val leido = repo.listarOutfits().getOrThrow().single()
        assertEquals(Ocasion.TRABAJO, leido.ocasion)
        assertEquals(listOf("2", "5"), leido.prendasIds)
        assertEquals(true, leido.favorito)
        assertEquals(ahora, leido.fecha)
    }

    @Test
    fun cambiarFavoritoYEliminar() = runTest {
        val original = Outfit("o1", Ocasion.CASUAL, listOf("1"), ahora, favorito = false)
        val repo = FakeOutfitsRepository(listOf(original))

        repo.cambiarFavorito("o1", true)
        assertEquals(true, repo.listarOutfits().getOrThrow().single().favorito)

        assertTrue(repo.cambiarFavorito("no-existe", true).isFailure)

        repo.eliminarOutfit("o1")
        assertEquals(emptyList(), repo.listarOutfits().getOrThrow())
    }
}
