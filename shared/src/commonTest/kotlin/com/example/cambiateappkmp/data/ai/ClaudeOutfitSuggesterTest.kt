package com.example.cambiateappkmp.data.ai

import com.example.cambiateappkmp.domain.config.ApiKeys
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class ApiKeysDePrueba(override val anthropic: String = "clave-de-prueba", override val maps: String = "") : ApiKeys

class ClaudeOutfitSuggesterTest {

    private val pedidos = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val fecha = Instant.fromEpochSeconds(1_763_000_000)

    private fun prenda(id: String, nombre: String, categoria: Categoria, ocasiones: List<Ocasion> = listOf(Ocasion.CASUAL), colores: List<String> = listOf("azul"), etiquetas: List<String> = emptyList()) =
        Prenda(id, nombre, "", categoria, colores, ocasiones, "", etiquetas)

    private val remera = prenda("id-remera", "Remera", Categoria.SUPERIOR, etiquetas = listOf("casual", "verano"))
    private val jean = prenda("id-jean", "Jean", Categoria.INFERIOR, colores = listOf("azul", "negro"))
    private val zapas = prenda("id-zapas", "Zapatillas", Categoria.CALZADO)
    private val basicas = listOf(remera, jean, zapas)

    private fun sugeridor(clave: String = "clave-de-prueba", manejador: MockRequestHandler): ClaudeOutfitSuggester {
        val cliente = HttpClient(MockEngine { pedido -> pedidos += pedido; manejador(pedido) })
        return ClaudeOutfitSuggester(ApiKeysDePrueba(clave), cliente, random = Random(1), ahora = { fecha })
    }

    private fun respuestaConTexto(texto: String) =
        Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(
                mapOf(
                    "content" to kotlinx.serialization.json.JsonArray(
                        listOf(JsonObject(mapOf("type" to kotlinx.serialization.json.JsonPrimitive("text"), "text" to kotlinx.serialization.json.JsonPrimitive(texto)))),
                    ),
                ),
            ),
        )

    private val respuestaValida get() = respuestaConTexto("""{"outfits":[{"prendasIds":["p1","p2","p3"]}]}""")

    private fun HttpRequestData.cuerpo(): JsonObject = Json.parseToJsonElement((body as TextContent).text).jsonObject

    private fun HttpRequestData.pedidoDeUsuario(): String =
        cuerpo()["messages"]!!.jsonArray.single().jsonObject["content"]!!.jsonPrimitive.content

    @Test
    fun mandaElPedidoALaApiDeAnthropicConLosHeadersYElModeloDeLaNativa() = runTest {
        val s = sugeridor { respond(respuestaValida, headers = json) }

        s.sugerir(Ocasion.CASUAL, basicas, null)

        val pedido = pedidos.single()
        assertEquals(HttpMethod.Post, pedido.method)
        assertEquals("https://api.anthropic.com/v1/messages", pedido.url.toString())
        assertEquals("clave-de-prueba", pedido.headers["x-api-key"])
        assertEquals("2023-06-01", pedido.headers["anthropic-version"])
        val cuerpo = pedido.cuerpo()
        assertEquals("claude-haiku-4-5-20251001", cuerpo["model"]!!.jsonPrimitive.content)
        assertEquals(1000, cuerpo["max_tokens"]!!.jsonPrimitive.int)
        assertEquals(ClaudeOutfitSuggester.INSTRUCCIONES, cuerpo["system"]!!.jsonPrimitive.content)
        assertEquals("user", cuerpo["messages"]!!.jsonArray.single().jsonObject["role"]!!.jsonPrimitive.content)
    }

    @Test
    fun laIaSoloVeAliasYNuncaLosIdsReales() = runTest {
        val s = sugeridor { respond(respuestaValida, headers = json) }

        s.sugerir(Ocasion.CASUAL, basicas, null)

        val texto = pedidos.single().pedidoDeUsuario()
        val lineas = texto.lines().filter { it.isNotEmpty() }
        assertEquals("Ocasion: casual", lineas.first())
        assertEquals(3, lineas.size - 1)
        assertTrue(lineas.drop(1).all { Regex("^p[1-3]\\|").containsMatchIn(it) })
        listOf("id-remera", "id-jean", "id-zapas").forEach { assertFalse(it in pedidos.single().pedidoDeUsuario(), it) }
        assertTrue(lineas.any { it == "p1|Remera|superior|azul|casual,verano" })
        assertTrue(lineas.any { it == "p2|Jean|inferior|azul,negro|" })
    }

    @Test
    fun sinClimaElPedidoEsIgualAlDeLaNativa() {
        val s = sugeridor { respond("{}", headers = json) }

        val pedido = s.armarPedido(Ocasion.TRABAJO, listOf("p1" to remera, "p2" to jean))

        assertEquals("Ocasion: trabajo\np1|Remera|superior|azul|casual,verano\np2|Jean|inferior|azul,negro|\n", pedido)
    }

    @Test
    fun conClimaSumaUnaLineaOpcional() {
        val s = sugeridor { respond("{}", headers = json) }

        val llueve = s.armarPedido(Ocasion.CASUAL, listOf("p1" to remera), ClimaActual(11.6, "Lluvia moderada", llueve = true))
        val seco = s.armarPedido(Ocasion.CASUAL, listOf("p1" to remera), ClimaActual(28.0, "Despejado", llueve = false))

        assertEquals("Ocasion: casual\nClima: 12°C, llueve\np1|Remera|superior|azul|casual,verano\n", llueve)
        assertEquals("Ocasion: casual\nClima: 28°C, despejado\np1|Remera|superior|azul|casual,verano\n", seco)
    }

    @Test
    fun elClimaSoloViajaSiSeLoPasan() = runTest {
        val s = sugeridor { respond(respuestaValida, headers = json) }

        s.sugerir(Ocasion.CASUAL, basicas, null)
        s.sugerir(Ocasion.CASUAL, basicas, ClimaActual(5.0, "Nublado", llueve = false))

        assertFalse("Clima:" in pedidos[0].pedidoDeUsuario())
        assertTrue("Clima: 5°C, nublado" in pedidos[1].pedidoDeUsuario())
    }

    @Test
    fun limpiaBarrasSaltosDeLineaYCortaEn40Caracteres() {
        val s = sugeridor { respond("{}", headers = json) }
        val rara = prenda("x", "Remera|con\nsaltos\ry barras", Categoria.SUPERIOR, colores = listOf("a|b"), etiquetas = listOf("x".repeat(60)))

        val linea = s.armarPedido(Ocasion.CASUAL, listOf("p1" to rara)).lines()[1]

        assertEquals("p1|Remera con saltos y barras|superior|a b|${"x".repeat(40)}", linea)
        assertEquals(5, linea.split("|").size)
    }

    @Test
    fun usaSoloPrendasDeLaOcasionYHastaOchoPorCategoria() = runTest {
        val s = sugeridor { respond(respuestaValida, headers = json) }
        val superiores = (1..12).map { prenda("s$it", "Sup$it", Categoria.SUPERIOR) }
        val deOtraOcasion = prenda("t1", "Traje", Categoria.SUPERIOR, ocasiones = listOf(Ocasion.CASAMIENTO))

        s.sugerir(Ocasion.CASUAL, superiores + deOtraOcasion + jean + zapas, null)

        val lineas = pedidos.single().pedidoDeUsuario().lines().filter { it.contains("|superior|") }
        assertEquals(8, lineas.size)
        assertFalse("Traje" in pedidos.single().pedidoDeUsuario())
    }

    @Test
    fun sinPrendasSuficientesDevuelveVacioSinLlamarALaRed() = runTest {
        val s = sugeridor { respond(respuestaValida, headers = json) }

        assertEquals(emptyList(), s.sugerir(Ocasion.CASUAL, listOf(remera, jean), null).getOrThrow())
        assertEquals(emptyList(), s.sugerir(Ocasion.TRABAJO, basicas, null).getOrThrow()) // ninguna es de trabajo
        assertEquals(emptyList(), s.sugerir(Ocasion.CASUAL, emptyList(), null).getOrThrow())
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun conLaClaveVaciaFallaPorPermisosSinLlamarALaRed() = runTest {
        val s = sugeridor(clave = "  ") { respond(respuestaValida, headers = json) }

        assertSame(DomainException.SinPermisos, s.sugerir(Ocasion.CASUAL, basicas, null).exceptionOrNull())
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun devuelveLosOutfitsConLosIdsRealesNoLosAlias() = runTest {
        val s = sugeridor { respond(respuestaValida, headers = json) }

        val outfits = s.sugerir(Ocasion.CASUAL, basicas, null).getOrThrow()

        val outfit = outfits.single()
        assertEquals(setOf("id-remera", "id-jean", "id-zapas"), outfit.prendasIds.toSet())
        assertEquals("", outfit.id)
        assertEquals(Ocasion.CASUAL, outfit.ocasion)
        assertEquals(fecha, outfit.fecha)
        assertFalse(outfit.favorito)
    }

    @Test
    fun los401Y403SonFaltaDePermisos() = runTest {
        listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden).forEach { estado ->
            val s = sugeridor { respond("{}", estado, json) }

            assertSame(DomainException.SinPermisos, s.sugerir(Ocasion.CASUAL, basicas, null).exceptionOrNull(), "con $estado")
        }
    }

    @Test
    fun otrosErroresHttpSonDesconocidos() = runTest {
        val s = sugeridor { respond("{}", HttpStatusCode.InternalServerError, json) }

        val error = s.sugerir(Ocasion.CASUAL, basicas, null).exceptionOrNull()

        assertIs<DomainException.Desconocido>(error)
        assertTrue(error.cause?.message.orEmpty().contains("500"))
    }

    @Test
    fun siNoLlegaRespuestaEsFaltaDeConexion() = runTest {
        val s = sugeridor { throw IllegalStateException("sin red") }

        assertSame(DomainException.SinConexion, s.sugerir(Ocasion.CASUAL, basicas, null).exceptionOrNull())
    }

    @Test
    fun siClaudeNoDevuelveOutfitsValidosEsUnError() = runTest {
        val s = sugeridor { respond(respuestaConTexto("""{"outfits":[{"prendasIds":["p1","p99"]}]}"""), headers = json) }

        assertIs<DomainException.Desconocido>(s.sugerir(Ocasion.CASUAL, basicas, null).exceptionOrNull())
    }

    @Test
    fun siLaRespuestaNoEsJsonEsUnErrorDesconocido() = runTest {
        val s = sugeridor { respond("<html>", headers = json) }

        assertIs<DomainException.Desconocido>(s.sugerir(Ocasion.CASUAL, basicas, null).exceptionOrNull())
    }
}
