package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.AnalisisPrenda
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.VocabularioEtiquetas
import com.example.cambiateappkmp.domain.repository.AnalizadorDePrenda
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

private class AnalizadorDePrueba(private val resultado: suspend () -> Result<AnalisisPrenda>) : AnalizadorDePrenda {
    var llamadas = 0
    var idioma: String? = null
    override suspend fun analizar(fotoUrl: String, idioma: String): Result<AnalisisPrenda> {
        llamadas++
        this.idioma = idioma
        return resultado()
    }
}

class AnalizarPrendaUseCaseTest {

    private fun cruda(etiquetas: List<String> = emptyList()) = AnalisisPrenda(
        nombre = "  Jean azul  ",
        descripcion = "  Recto  ",
        categoria = Categoria.INFERIOR,
        colores = listOf(" Azul ", "AZUL", "", "negro", "gris", "blanco", "rojo"),
        ocasiones = listOf(Ocasion.CASUAL, Ocasion.CASUAL, Ocasion.FACULTAD),
        etiquetas = etiquetas,
    )

    @Test
    fun normalizaLosTextosLosColoresYLasOcasiones() = runTest {
        val analizador = AnalizadorDePrueba { Result.success(cruda()) }

        val analisis = AnalizarPrendaUseCase(analizador)("https://x/f.jpg").getOrThrow()

        assertEquals("Jean azul", analisis.nombre)
        assertEquals("Recto", analisis.descripcion)
        assertEquals(listOf("azul", "negro", "gris", "blanco"), analisis.colores) // sin repetidos, hasta 4
        assertEquals(listOf(Ocasion.CASUAL, Ocasion.FACULTAD), analisis.ocasiones)
        assertEquals(Categoria.INFERIOR, analisis.categoria)
    }

    @Test
    fun recortaElNombreA40YLaDescripcionA120() = runTest {
        val larga = cruda().copy(nombre = "n".repeat(60), descripcion = "d".repeat(200))

        val analisis = AnalizarPrendaUseCase(AnalizadorDePrueba { Result.success(larga) })("https://x/f.jpg").getOrThrow()

        assertEquals(40, analisis.nombre.length)
        assertEquals(120, analisis.descripcion.length)
    }

    @Test
    fun soloDejaLasEtiquetasDelVocabularioSinRepetirYEnMinuscula() = runTest {
        val crudas = cruda(listOf("Casual", "jean", "  RECTO ", "inventada", "casual", "", "cinturaalta"))

        val analisis = AnalizarPrendaUseCase(AnalizadorDePrueba { Result.success(crudas) })("https://x/f.jpg").getOrThrow()

        assertEquals(listOf("casual", "jean", "recto", "cinturaalta"), analisis.etiquetas)
    }

    @Test
    fun seQuedaConComoMuchoDoceEtiquetas() = runTest {
        val todas = VocabularioEtiquetas.PERMITIDAS.toList().take(30)

        val analisis = AnalizarPrendaUseCase(AnalizadorDePrueba { Result.success(cruda(todas)) })("https://x/f.jpg").getOrThrow()

        assertEquals(12, analisis.etiquetas.size)
        assertEquals(todas.take(12), analisis.etiquetas)
    }

    @Test
    fun sinFotoNoLlamaAlAnalizador() = runTest {
        val analizador = AnalizadorDePrueba { Result.success(cruda()) }

        assertTrue(AnalizarPrendaUseCase(analizador)("  ").isFailure)
        assertEquals(0, analizador.llamadas)
    }

    @Test
    fun pasaElIdiomaYPropagaLosErroresDelAnalizador() = runTest {
        val analizador = AnalizadorDePrueba { Result.failure(DomainException.SinPermisos) }

        val resultado = AnalizarPrendaUseCase(analizador)("https://x/f.jpg", "en")

        assertSame(DomainException.SinPermisos, resultado.exceptionOrNull())
        assertEquals("en", analizador.idioma)
    }

    @Test
    fun siTardaMasDeVeinteSegundosFalla() = runTest {
        val lento = AnalizadorDePrueba {
            delay(25_000)
            Result.success(cruda())
        }

        val resultado = AnalizarPrendaUseCase(lento)("https://x/f.jpg")

        assertIs<DomainException.Desconocido>(resultado.exceptionOrNull())
    }

    @Test
    fun siRespondeAntesDeVeinteSegundosNoFalla() = runTest {
        val casiLento = AnalizadorDePrueba {
            delay(19_000)
            Result.success(cruda())
        }

        assertTrue(AnalizarPrendaUseCase(casiLento)("https://x/f.jpg").isSuccess)
    }

    @Test
    fun etiquetarDevuelveSoloLasEtiquetasYVacioSiFalla() = runTest {
        val bien = AnalizadorDePrueba { Result.success(cruda(listOf("casual", "jean", "xxx"))) }
        val mal = AnalizadorDePrueba { Result.failure(DomainException.SinConexion) }

        assertEquals(listOf("casual", "jean"), EtiquetarPrendaUseCase(AnalizarPrendaUseCase(bien))("https://x/f.jpg"))
        assertEquals(emptyList(), EtiquetarPrendaUseCase(AnalizarPrendaUseCase(mal))("https://x/f.jpg"))
        assertEquals(emptyList(), EtiquetarPrendaUseCase(AnalizarPrendaUseCase(bien))(""))
    }

    @Test
    fun elVocabularioEsCerradoSinRepetidosNiTildesNiEspacios() {
        val palabras = VocabularioEtiquetas.PERMITIDAS

        assertEquals(114, palabras.size)
        assertEquals(12, VocabularioEtiquetas.MAXIMO_POR_PRENDA)
        assertTrue(palabras.all { p -> p.isNotEmpty() && p.all { it in 'a'..'z' } }, "solo letras minúsculas sin tildes")
        assertFalse("ñ" in palabras.joinToString(""))
        assertTrue(listOf("casual", "formal", "verano", "invierno", "otono", "primavera", "jean", "versatil").all { it in palabras })
    }
}
