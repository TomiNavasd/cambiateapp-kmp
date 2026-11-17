package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.data.remote.OutfitDto
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

class OutfitMapperTest {

    private val fecha = Instant.fromEpochSeconds(1_763_000_000, 250)
    private val outfit = Outfit("o1", Ocasion.SALIDA, listOf("a", "b", "c"), fecha, favorito = true)

    @Test
    fun idaYVueltaConservaTodo() {
        assertEquals(outfit, outfit.aDto("uid1").aDomain("o1"))
    }

    @Test
    fun elDtoUsaLosValoresYCamposDeLaNativa() {
        val dto = outfit.aDto("uid1")

        assertEquals("uid1", dto.userId)
        assertEquals("salida", dto.ocasion)
        assertEquals(listOf("a", "b", "c"), dto.prendasIds)
        assertEquals(Timestamp(1_763_000_000, 250), dto.fecha)
        assertEquals(true, dto.favorito)
    }

    @Test
    fun laOcasionDeLaNativaSeLeeSinImportarMayusculas() {
        assertEquals(Ocasion.CASAMIENTO, OutfitDto(ocasion = "CASAMIENTO").aDomain("x")?.ocasion)
        assertEquals(Ocasion.DEPORTE, OutfitDto(ocasion = "deporte").aDomain("x")?.ocasion)
    }

    @Test
    fun unaOcasionDesconocidaDescartaElOutfit() {
        assertNull(OutfitDto(ocasion = "fiesta").aDomain("x"))
    }

    @Test
    fun sinFechaQuedaLaEpoca() {
        val leido = OutfitDto(ocasion = "casual", prendasIds = listOf("p")).aDomain("x")!!

        assertEquals(Instant.fromEpochSeconds(0), leido.fecha)
        assertFalse(leido.favorito)
    }

    @Test
    fun losCamposEditablesNoIncluyenNiElDuenoNiLaFecha() {
        assertEquals(listOf("ocasion", "prendasIds", "favorito"), CAMPOS_EDITABLES_OUTFIT)
    }

    @Test
    fun seOrdenaDelMasNuevoAlMasViejo() {
        val viejo = outfit.copy(id = "v", fecha = Instant.fromEpochSeconds(100))
        val nuevo = outfit.copy(id = "n", fecha = Instant.fromEpochSeconds(900))
        val medio = outfit.copy(id = "m", fecha = Instant.fromEpochSeconds(500))

        assertEquals(listOf("n", "m", "v"), listOf(viejo, nuevo, medio).ordenadosPorFecha().map { it.id })
    }
}
