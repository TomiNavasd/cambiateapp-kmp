package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class OutfitsRepositoryJvmTest {

    private val repo = OutfitsRepositoryJvm()
    private val outfit = Outfit("o1", Ocasion.CASUAL, listOf("1"), Instant.fromEpochSeconds(0), favorito = false)

    private fun mensajeDe(resultado: Result<*>): String? {
        val error = resultado.exceptionOrNull()
        assertIs<DomainException.Desconocido>(error)
        return error.cause?.message
    }

    @Test
    fun noHayOutfitsGuardadosEnEscritorio() = runTest {
        assertEquals(emptyList(), repo.observarOutfits().first())
    }

    @Test
    fun todasLasOperacionesDicenQueNoEstanDisponibles() = runTest {
        assertEquals(MENSAJE_OUTFITS_NO_DISPONIBLE, mensajeDe(repo.listarOutfits()))
        assertEquals(MENSAJE_OUTFITS_NO_DISPONIBLE, mensajeDe(repo.guardarOutfit(outfit)))
        assertEquals(MENSAJE_OUTFITS_NO_DISPONIBLE, mensajeDe(repo.eliminarOutfit("o1")))
        assertEquals(MENSAJE_OUTFITS_NO_DISPONIBLE, mensajeDe(repo.cambiarFavorito("o1", true)))
        assertEquals("No disponible todavía en escritorio", MENSAJE_OUTFITS_NO_DISPONIBLE)
    }
}
