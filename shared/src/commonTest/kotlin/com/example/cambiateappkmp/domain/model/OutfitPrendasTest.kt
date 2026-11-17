package com.example.cambiateappkmp.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OutfitPrendasTest {

    private fun prenda(nombre: String, categoria: Categoria, vararg etiquetas: String) = Prenda(
        id = nombre,
        nombre = nombre,
        descripcion = "",
        categoria = categoria,
        colores = emptyList(),
        ocasiones = listOf(Ocasion.CASUAL),
        fotoUrl = "",
        etiquetas = etiquetas.toList(),
    )

    private val remera = prenda("Remera", Categoria.SUPERIOR, "verano", "primavera")
    private val jean = prenda("Jean", Categoria.INFERIOR, "verano", "otono", "primavera")
    private val zapas = prenda("Zapatillas", Categoria.CALZADO)

    @Test
    fun nombreConSuperiorEInferior() {
        assertEquals("Remera con Jean", listOf(zapas, jean, remera).nombreVisibleDeOutfit())
    }

    @Test
    fun nombreConSoloUnaDeLasDos() {
        assertEquals("Remera", listOf(remera, zapas).nombreVisibleDeOutfit())
        assertEquals("Jean", listOf(jean, zapas).nombreVisibleDeOutfit())
    }

    @Test
    fun nombreGenericoSinSuperiorNiInferior() {
        assertEquals("Outfit", listOf(zapas).nombreVisibleDeOutfit())
        assertEquals("Outfit", emptyList<Prenda>().nombreVisibleDeOutfit())
    }

    @Test
    fun temporadasEsLaInterseccionEnElOrdenDeLasTemporadas() {
        assertEquals(listOf(Temporada.PRIMAVERA, Temporada.VERANO), listOf(remera, jean).temporadasComunes())
        assertEquals("Primavera / Verano", listOf(remera, jean).temporadaTextoDeOutfit())
    }

    @Test
    fun lasPrendasSinEtiquetasDeTemporadaNoCuentan() {
        assertEquals(listOf(Temporada.PRIMAVERA, Temporada.VERANO), listOf(remera, zapas, jean).temporadasComunes())
    }

    @Test
    fun sinDatosOSinCoincidenciaNoHayTemporada() {
        assertNull(listOf(zapas).temporadaTextoDeOutfit())
        assertNull(emptyList<Prenda>().temporadaTextoDeOutfit())

        val invierno = prenda("Sobretodo", Categoria.ABRIGO, "invierno")
        assertEquals(emptyList(), listOf(remera, invierno).temporadasComunes())
        assertNull(listOf(remera, invierno).temporadaTextoDeOutfit())
    }

    @Test
    fun conLasCuatroEsTodoElAnio() {
        val todo = prenda("Campera liviana", Categoria.ABRIGO, "primavera", "verano", "otono", "invierno")
        assertEquals("Todo el año", listOf(todo).temporadaTextoDeOutfit())
        assertEquals("Todo el año", textoDeTemporadas(Temporada.entries))
    }

    @Test
    fun losValoresDeFirestoreDeLasOcasionesSonLosDeLaNativa() {
        assertEquals(
            listOf("trabajo", "facultad", "salida", "casamiento", "deporte", "casual"),
            Ocasion.entries.map { it.valorFirestore },
        )
        assertEquals(Ocasion.SALIDA, ocasionDeValorFirestore("SALIDA"))
        assertEquals(Ocasion.CASUAL, ocasionDeValorFirestore("casual"))
        assertNull(ocasionDeValorFirestore("fiesta"))
    }
}
