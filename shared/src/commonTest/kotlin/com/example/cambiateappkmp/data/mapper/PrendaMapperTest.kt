package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.data.remote.PrendaDto
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrendaMapperTest {

    private val prenda = Prenda(
        id = "p1",
        nombre = "Jean",
        descripcion = "Recto",
        categoria = Categoria.INFERIOR,
        colores = listOf("azul"),
        ocasiones = listOf(Ocasion.CASUAL, Ocasion.FACULTAD),
        fotoUrl = "https://x/y.jpg",
        etiquetas = listOf("casual", "jean", "versatil"),
    )

    @Test
    fun idaYVueltaConservaLasEtiquetas() {
        val dto = prenda.aDto("uid1")

        assertEquals(listOf("casual", "jean", "versatil"), dto.etiquetas)
        assertEquals(prenda, dto.aDomain("p1"))
    }

    @Test
    fun idaYVueltaSinEtiquetas() {
        val sinEtiquetas = prenda.copy(etiquetas = emptyList())

        val dto = sinEtiquetas.aDto("uid1")

        assertEquals(emptyList(), dto.etiquetas)
        assertEquals(sinEtiquetas, dto.aDomain("p1"))
    }

    @Test
    fun unDocumentoDeLaNativaSinEtiquetasSeLeeConListaVacia() {
        val dto = PrendaDto(userId = "u", nombre = "Remera", categoria = "superior")

        assertEquals(emptyList(), dto.aDomain("x")?.etiquetas)
    }

    @Test
    fun unaPrendaCreadaEnCodigoNoTraeEtiquetasPorDefecto() {
        val nueva = Prenda("", "n", "", Categoria.SUPERIOR, emptyList(), emptyList(), "")

        assertEquals(emptyList(), nueva.etiquetas)
    }

    @Test
    fun unaCategoriaDesconocidaSigueDescartandoLaPrenda() {
        assertNull(PrendaDto(categoria = "rara", etiquetas = listOf("a")).aDomain("x"))
    }
}
