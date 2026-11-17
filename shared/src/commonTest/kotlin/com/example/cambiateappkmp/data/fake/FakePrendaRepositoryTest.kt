package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class FakePrendaRepositoryTest {

    private val nueva = Prenda(
        id = "",
        nombre = "Bufanda",
        descripcion = "De lana",
        categoria = Categoria.ACCESORIO,
        colores = listOf("gris"),
        ocasiones = listOf(Ocasion.CASUAL),
        fotoUrl = "https://x/b.jpg",
        etiquetas = listOf("lana", "invierno"),
    )

    @Test
    fun arrancaConDiezPrendasDeEjemploConEtiquetas() = runTest {
        val prendas = FakePrendaRepository().observarPrendas().first()

        assertEquals((1..10).map { it.toString() }, prendas.map { it.id })
        assertTrue(prendas.all { it.etiquetas.isNotEmpty() && it.fotoUrl.isNotBlank() })
        assertEquals(Categoria.entries.toSet(), prendas.map { it.categoria }.toSet())
    }

    @Test
    fun obtenerDevuelveLaPrendaONullSiNoExiste() = runTest {
        val repo = FakePrendaRepository()

        assertEquals("Jean azul", repo.obtenerPrenda("4")?.nombre)
        assertNull(repo.obtenerPrenda("999"))
    }

    @Test
    fun guardarUnaNuevaLeDaUnIdYLaAgregaConSusEtiquetas() = runTest {
        val repo = FakePrendaRepository()

        assertTrue(repo.guardarPrenda(nueva).isSuccess)

        val prendas = repo.observarPrendas().first()
        val agregada = prendas.last()
        assertEquals(11, prendas.size)
        assertNotEquals("", agregada.id)
        assertEquals("Bufanda", agregada.nombre)
        assertEquals(listOf("lana", "invierno"), agregada.etiquetas)
    }

    @Test
    fun cadaPrendaNuevaRecibeUnIdDistinto() = runTest {
        val repo = FakePrendaRepository()

        repo.guardarPrenda(nueva)
        repo.guardarPrenda(nueva.copy(nombre = "Gorro"))

        val ids = repo.observarPrendas().first().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun editarReemplazaLaPrendaSinAgregarOtra() = runTest {
        val repo = FakePrendaRepository()
        val original = repo.obtenerPrenda("4")!!

        repo.guardarPrenda(original.copy(nombre = "Jean negro", colores = listOf("negro")))

        val prendas = repo.observarPrendas().first()
        assertEquals(10, prendas.size)
        assertEquals("Jean negro", prendas.first { it.id == "4" }.nombre)
        assertEquals(original.etiquetas, prendas.first { it.id == "4" }.etiquetas)
    }

    @Test
    fun editarUnaPrendaQueNoExisteNoCambiaNada() = runTest {
        val repo = FakePrendaRepository()
        val antes = repo.observarPrendas().first()

        repo.guardarPrenda(nueva.copy(id = "no-existe"))

        assertEquals(antes, repo.observarPrendas().first())
    }
}
