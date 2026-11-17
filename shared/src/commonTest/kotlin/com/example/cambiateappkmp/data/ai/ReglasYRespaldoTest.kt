package com.example.cambiateappkmp.data.ai

import com.example.cambiateappkmp.data.fake.FakeOutfitSuggester
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.OutfitSuggester
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class ReglasYRespaldoTest {

    private val fecha = Instant.fromEpochSeconds(1_763_000_000)

    private fun prenda(id: String, categoria: Categoria, ocasiones: List<Ocasion> = listOf(Ocasion.CASUAL)) =
        Prenda(id, id, "", categoria, listOf("azul"), ocasiones, "")

    private fun reglas() = ReglasOutfitSuggester(Random(3)) { fecha }

    private val armario = listOf(
        prenda("s1", Categoria.SUPERIOR), prenda("s2", Categoria.SUPERIOR),
        prenda("i1", Categoria.INFERIOR), prenda("i2", Categoria.INFERIOR),
        prenda("c1", Categoria.CALZADO),
        prenda("a1", Categoria.ABRIGO),
        prenda("x1", Categoria.ACCESORIO), prenda("x2", Categoria.ACCESORIO),
    )

    @Test
    fun cadaOutfitTieneSuperiorInferiorYCalzadoMasAbrigoYAccesorio() = runTest {
        val outfits = reglas().sugerir(Ocasion.CASUAL, armario, null).getOrThrow()
        val porId = armario.associateBy { it.id }

        assertEquals(4, outfits.size) // 2 superiores x 2 inferiores x 1 calzado
        outfits.forEach { outfit ->
            val categorias = outfit.prendasIds.map { porId.getValue(it).categoria }
            assertEquals(1, categorias.count { it == Categoria.SUPERIOR })
            assertEquals(1, categorias.count { it == Categoria.INFERIOR })
            assertEquals(1, categorias.count { it == Categoria.CALZADO })
            assertEquals(1, categorias.count { it == Categoria.ABRIGO })
            assertEquals(1, categorias.count { it == Categoria.ACCESORIO })
            assertEquals("", outfit.id)
            assertEquals(fecha, outfit.fecha)
        }
        assertEquals(4, outfits.map { it.prendasIds.toSet() }.toSet().size, "sin repetidos")
    }

    @Test
    fun losAccesoriosRotanEntreLosOutfits() = runTest {
        val outfits = reglas().sugerir(Ocasion.CASUAL, armario, null).getOrThrow()
        val accesorios = outfits.map { o -> o.prendasIds.single { it.startsWith("x") } }

        assertEquals(setOf("x1", "x2"), accesorios.toSet())
    }

    @Test
    fun hastaDiezOutfitsYCuatroPorCategoria() = runTest {
        val muchas = listOf(Categoria.SUPERIOR, Categoria.INFERIOR, Categoria.CALZADO).flatMap { c ->
            (1..9).map { prenda("${c.name}$it", c) }
        }

        val outfits = reglas().sugerir(Ocasion.CASUAL, muchas, null).getOrThrow()

        assertEquals(10, outfits.size)
        for (categoria in listOf(Categoria.SUPERIOR, Categoria.INFERIOR, Categoria.CALZADO)) {
            val usadas = outfits.flatMap { it.prendasIds }.filter { it.startsWith(categoria.name) }.toSet()
            assertTrue(usadas.size <= 4, "$categoria usó ${usadas.size}")
        }
    }

    @Test
    fun sinSuperiorInferiorOCalzadoDeLaOcasionDevuelveVacio() = runTest {
        assertEquals(emptyList(), reglas().sugerir(Ocasion.CASUAL, armario.filter { it.categoria != Categoria.CALZADO }, null).getOrThrow())
        assertEquals(emptyList(), reglas().sugerir(Ocasion.TRABAJO, armario, null).getOrThrow())
        assertEquals(emptyList(), reglas().sugerir(Ocasion.CASUAL, emptyList(), null).getOrThrow())
    }

    private class Fijo(private val resultado: Result<List<Outfit>>) : OutfitSuggester {
        var llamadas = 0
        var climaRecibido: ClimaActual? = null
        override suspend fun sugerir(ocasion: Ocasion, prendas: List<Prenda>, clima: ClimaActual?): Result<List<Outfit>> {
            llamadas++
            climaRecibido = clima
            return resultado
        }
    }

    private val unOutfit = Outfit("", Ocasion.CASUAL, listOf("a"), fecha, false)
    private val deRespaldo = Outfit("", Ocasion.CASUAL, listOf("b"), fecha, false)

    @Test
    fun siClaudeResponderUsaLoDeClaudeYNoLasReglas() = runTest {
        val respaldo = Fijo(Result.success(listOf(deRespaldo)))

        val resultado = SugeridorConRespaldo(Fijo(Result.success(listOf(unOutfit))), respaldo).sugerir(Ocasion.CASUAL, armario, null)

        assertEquals(listOf(unOutfit), resultado.getOrThrow())
        assertEquals(0, respaldo.llamadas)
    }

    @Test
    fun siClaudeFallaUsaLasReglasYAvisaDelError() = runTest {
        val error = DomainException.SinConexion
        val avisados = mutableListOf<Throwable>()
        val respaldo = Fijo(Result.success(listOf(deRespaldo)))
        val clima = ClimaActual(10.0, "Nublado", false)

        val resultado = SugeridorConRespaldo(Fijo(Result.failure(error)), respaldo) { avisados += it }
            .sugerir(Ocasion.CASUAL, armario, clima)

        assertEquals(listOf(deRespaldo), resultado.getOrThrow())
        assertSame(error, avisados.single())
        assertSame(clima, respaldo.climaRecibido)
    }

    @Test
    fun siClaudeVieneVacioUsaLasReglasSinAvisarError() = runTest {
        val avisados = mutableListOf<Throwable>()
        val respaldo = Fijo(Result.success(listOf(deRespaldo)))

        val resultado = SugeridorConRespaldo(Fijo(Result.success(emptyList())), respaldo) { avisados += it }
            .sugerir(Ocasion.CASUAL, armario, null)

        assertEquals(listOf(deRespaldo), resultado.getOrThrow())
        assertTrue(avisados.isEmpty())
    }

    @Test
    fun conClaveVaciaLaAppSigueSugiriendoConLasReglas() = runTest {
        val sinClave = ClaudeOutfitSuggester(ApiKeysDePrueba(anthropic = ""), io.ktor.client.HttpClient(io.ktor.client.engine.mock.MockEngine { error("no debe llamar a la red") }))

        val resultado = SugeridorConRespaldo(sinClave, reglas()).sugerir(Ocasion.CASUAL, armario, null)

        assertEquals(4, resultado.getOrThrow().size)
    }

    @Test
    fun elFakeDevuelveSiempreLoMismoParaLasMismasPrendas() = runTest {
        val fake = FakeOutfitSuggester(demoraMs = 0)

        val primera = fake.sugerir(Ocasion.CASUAL, armario, null).getOrThrow().map { it.prendasIds }
        val segunda = fake.sugerir(Ocasion.CASUAL, armario, null).getOrThrow().map { it.prendasIds }

        assertTrue(primera.isNotEmpty())
        assertEquals(primera, segunda)
    }
}
