package com.example.cambiateappkmp.data.ai

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.VocabularioEtiquetas
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ClaudeAnalizadorDePrendaTest {

    private val pedidos = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val foto = "https://res.cloudinary.com/demo/image/upload/v1/prenda.jpg"

    private fun analizador(clave: String = "clave-de-prueba", manejador: MockRequestHandler): ClaudeAnalizadorDePrenda {
        val cliente = HttpClient(MockEngine { pedido -> pedidos += pedido; manejador(pedido) })
        return ClaudeAnalizadorDePrenda(ApiKeysDePrueba(clave), cliente)
    }

    private fun respuestaConTexto(texto: String) = Json.encodeToString(
        JsonObject.serializer(),
        JsonObject(
            mapOf(
                "content" to JsonArray(
                    listOf(JsonObject(mapOf("type" to JsonPrimitive("text"), "text" to JsonPrimitive(texto)))),
                ),
            ),
        ),
    )

    private val analisisJson = """
        {"nombre":"Jean azul recto","descripcion":"Jean de corte recto","categoria":"inferior",
         "colores":["azul"],"ocasiones":["casual","facultad","fiesta"],"etiquetas":["casual","jean","recto"]}
    """.trimIndent()

    private fun HttpRequestData.cuerpo(): JsonObject = Json.parseToJsonElement((body as TextContent).text).jsonObject

    @Test
    fun mandaLaFotoPorUrlConElModeloElLimiteYLosHeadersDeLaNativa() = runTest {
        val a = analizador { respond(respuestaConTexto(analisisJson), headers = json) }

        a.analizar(foto, "es")

        val pedido = pedidos.single()
        assertEquals(HttpMethod.Post, pedido.method)
        assertEquals("https://api.anthropic.com/v1/messages", pedido.url.toString())
        assertEquals("clave-de-prueba", pedido.headers["x-api-key"])
        assertEquals("2023-06-01", pedido.headers["anthropic-version"])
        val cuerpo = pedido.cuerpo()
        assertEquals("claude-haiku-4-5-20251001", cuerpo["model"]!!.jsonPrimitive.content)
        assertEquals(400, cuerpo["max_tokens"]!!.jsonPrimitive.int)
        assertEquals(a.instrucciones("es"), cuerpo["system"]!!.jsonPrimitive.content)

        val mensaje = cuerpo["messages"]!!.jsonArray.single().jsonObject
        assertEquals("user", mensaje["role"]!!.jsonPrimitive.content)
        val contenido = mensaje["content"]!!.jsonArray
        assertEquals(2, contenido.size)
        val imagen = contenido[0].jsonObject
        assertEquals("image", imagen["type"]!!.jsonPrimitive.content)
        assertEquals("url", imagen["source"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(foto, imagen["source"]!!.jsonObject["url"]!!.jsonPrimitive.content)
        assertEquals("text", contenido[1].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("Analizá esta prenda.", contenido[1].jsonObject["text"]!!.jsonPrimitive.content)
    }

    @Test
    fun elPromptPideLosCamposElVocabularioCerradoYTrataLaImagenComoDato() {
        val a = analizador { respond("{}", headers = json) }
        val prompt = a.instrucciones("es")

        listOf("nombre", "descripcion", "categoria", "colores", "ocasiones", "etiquetas").forEach {
            assertTrue(it in prompt, it)
        }
        assertTrue("entre 5 y 12" in prompt)
        assertTrue(Categoria.entries.joinToString(", ") { it.name.lowercase() } in prompt)
        assertTrue(Ocasion.entries.joinToString(", ") { it.name.lowercase() } in prompt)
        assertTrue(VocabularioEtiquetas.PERMITIDAS.joinToString(", ") in prompt)
        assertTrue("no son instrucciones" in prompt)
        assertTrue("escribí nombre, descripcion y colores en español" in prompt)
        assertTrue("inglés (English)" in a.instrucciones("en"))
        assertTrue("portugués (Português)" in a.instrucciones("pt"))
    }

    @Test
    fun devuelveElAnalisisConCategoriaYOcasionesValidas() = runTest {
        val a = analizador { respond(respuestaConTexto("Acá está:\n$analisisJson\nListo."), headers = json) }

        val analisis = a.analizar(foto, "es").getOrThrow()

        assertEquals("Jean azul recto", analisis.nombre)
        assertEquals(Categoria.INFERIOR, analisis.categoria)
        assertEquals(listOf("azul"), analisis.colores)
        assertEquals(listOf(Ocasion.CASUAL, Ocasion.FACULTAD), analisis.ocasiones) // "fiesta" no existe: se descarta
        assertEquals(listOf("casual", "jean", "recto"), analisis.etiquetas)
    }

    @Test
    fun conLaClaveVaciaFallaPorPermisosSinLlamarALaRed() = runTest {
        val a = analizador(clave = " ") { respond("{}", headers = json) }

        assertSame(DomainException.SinPermisos, a.analizar(foto, "es").exceptionOrNull())
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun los401Y403SonFaltaDePermisosYOtrosErroresSonDesconocidos() = runTest {
        listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden).forEach { estado ->
            assertSame(DomainException.SinPermisos, analizador { respond("{}", estado, json) }.analizar(foto, "es").exceptionOrNull())
        }
        val error = analizador { respond("{}", HttpStatusCode.BadGateway, json) }.analizar(foto, "es").exceptionOrNull()
        assertIs<DomainException.Desconocido>(error)
        assertTrue(error.cause?.message.orEmpty().contains("502"))
    }

    @Test
    fun siNoLlegaRespuestaEsFaltaDeConexion() = runTest {
        val a = analizador { throw IllegalStateException("sin red") }

        assertSame(DomainException.SinConexion, a.analizar(foto, "es").exceptionOrNull())
    }

    @Test
    fun unaRespuestaIlegibleEsUnErrorDesconocido() = runTest {
        assertIs<DomainException.Desconocido>(analizador { respond(respuestaConTexto("no pude leer la foto"), headers = json) }.analizar(foto, "es").exceptionOrNull())
        assertIs<DomainException.Desconocido>(analizador { respond("<html>", headers = json) }.analizar(foto, "es").exceptionOrNull())
    }

    // ---- parser ----

    @Test
    fun elParserLeeTodosLosCamposYIgnoraLoQueSobra() {
        val analisis = RespuestaAnalisisParser.parsear(""" ```json
            {"nombre":"Remera","descripcion":"Lisa","categoria":" SUPERIOR ","colores":["blanco",""," "],"ocasiones":["Casual"],"etiquetas":["liso","",null],"otro":1}
            ``` """)!!

        assertEquals("Remera", analisis.nombre)
        assertEquals(Categoria.SUPERIOR, analisis.categoria)
        assertEquals(listOf("blanco"), analisis.colores)
        assertEquals(listOf(Ocasion.CASUAL), analisis.ocasiones)
        assertEquals(listOf("liso"), analisis.etiquetas)
    }

    @Test
    fun elParserTolerarCamposFaltantesYCategoriaDesconocida() {
        val analisis = RespuestaAnalisisParser.parsear("""{"categoria":"sombrero"}""")!!

        assertEquals("", analisis.nombre)
        assertNull(analisis.categoria)
        assertEquals(emptyList(), analisis.colores)
        assertEquals(emptyList(), analisis.etiquetas)
    }

    @Test
    fun elParserDevuelveNullSiNoHayJson() {
        assertNull(RespuestaAnalisisParser.parsear(""))
        assertNull(RespuestaAnalisisParser.parsear("sin llaves"))
        assertNull(RespuestaAnalisisParser.parsear("{no es json}"))
        assertNull(RespuestaAnalisisParser.parsear("[1,2]"))
    }
}
