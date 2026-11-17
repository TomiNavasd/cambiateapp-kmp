package com.example.cambiateappkmp.presentation.wardrobe

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusquedaPrendasTest {

    private fun prenda(id: String, nombre: String, categoria: Categoria, vararg colores: String) = Prenda(
        id = id,
        nombre = nombre,
        descripcion = "",
        categoria = categoria,
        colores = colores.toList(),
        ocasiones = listOf(Ocasion.CASUAL),
        fotoUrl = "",
    )

    private val camisa = prenda("1", "Camisa Cuadrillé", Categoria.SUPERIOR, "Azul")
    private val jean = prenda("2", "Jean", Categoria.INFERIOR, "azul", "negro")
    private val zapatillas = prenda("3", "Zapatillas", Categoria.CALZADO, "blanco")

    @Test
    fun noDistingueMayusculasNiTildes() {
        assertTrue(camisa.coincideConBusqueda("CUADRILLE"))
        assertTrue(camisa.coincideConBusqueda("cuadrillé"))
    }

    @Test
    fun buscaPorNombreCategoriaYColor() {
        assertTrue(jean.coincideConBusqueda("jea"))
        assertTrue(zapatillas.coincideConBusqueda("calzado"))
        assertTrue(jean.coincideConBusqueda("negro"))
        assertFalse(jean.coincideConBusqueda("rojo"))
    }

    @Test
    fun buscaPorEtiquetasQueElUsuarioNoVe() {
        val conEtiquetas = jean.copy(etiquetas = listOf("versatil", "cinturaalta", "otono"))

        assertTrue(conEtiquetas.coincideConBusqueda("versátil"))
        assertTrue(conEtiquetas.coincideConBusqueda("OTOÑO"))
        assertTrue(conEtiquetas.coincideConBusqueda("cinturaalta"))
        assertFalse(jean.coincideConBusqueda("versatil"))
    }

    @Test
    fun laBusquedaPorEtiquetaSeCombinaConElFiltroDeCategoria() {
        val remera = camisa.copy(etiquetas = listOf("verano"))
        val short = jean.copy(etiquetas = listOf("verano"))
        val todas = listOf(remera, short, zapatillas)

        assertEquals(listOf(remera, short), todas.filtrarArmario(null, "verano"))
        assertEquals(listOf(short), todas.filtrarArmario(Categoria.INFERIOR, "verano"))
    }

    @Test
    fun laConsultaVaciaOEnBlancoDejaPasarTodo() {
        assertTrue(jean.coincideConBusqueda(""))
        assertTrue(jean.coincideConBusqueda("   "))
    }

    @Test
    fun primeroFiltraPorCategoriaYDespuesPorTexto() {
        val todas = listOf(camisa, jean, zapatillas)

        assertEquals(listOf(camisa, jean), todas.filtrarArmario(null, "azul"))
        assertEquals(listOf(jean), todas.filtrarArmario(Categoria.INFERIOR, "azul"))
        assertEquals(emptyList(), todas.filtrarArmario(Categoria.CALZADO, "azul"))
        assertEquals(listOf(zapatillas), todas.filtrarArmario(Categoria.CALZADO, ""))
    }

    @Test
    fun normalizarSacaTildesYEnies() {
        assertEquals("camion nino", normalizarBusqueda("  Camión NIÑO "))
    }
}
