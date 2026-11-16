package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.config.ConfigFirebaseEscritorio
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.model.Usuario
import com.example.cambiateappkmp.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** runTest salta el tiempo virtual mientras espera al motor HTTP; con Default los timeouts corren en tiempo real. */
private fun pruebaReal(bloque: suspend CoroutineScope.() -> Unit) =
    runTest { withContext(Dispatchers.Default) { bloque() } }

private class TokensDePrueba(var uidActual: String? = "uid1") : TokenProvider {
    var token = "TOKEN1"
    var tokenRenovado = "TOKEN2"
    var renovaciones = 0

    override val uid: String? get() = uidActual

    override suspend fun idTokenVigente(forzar: Boolean): Result<String> {
        if (uidActual == null) return Result.failure(DomainException.SesionNoIniciada)
        if (forzar) {
            renovaciones++
            token = tokenRenovado
        }
        return Result.success(token)
    }
}

private class AuthDePrueba(usuario: Usuario?) : AuthRepository {
    val flujo = MutableStateFlow(usuario)
    override val usuarioActual: Flow<Usuario?> = flujo
    override suspend fun login(email: String, pass: String) = Result.failure<Usuario>(UnsupportedOperationException())
    override suspend fun registrar(email: String, pass: String) = Result.failure<Usuario>(UnsupportedOperationException())
    override suspend fun cerrarSesion() { flujo.value = null }
}

class PrendaRepositoryRestTest {

    private val config = ConfigFirebaseEscritorio(apiKey = "clave", projectId = "proyecto")
    private val tokens = TokensDePrueba()
    private val auth = AuthDePrueba(Usuario("uid1", "ana@test.com"))
    private val pedidos = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repo(
        config: ConfigFirebaseEscritorio = this.config,
        manejador: MockRequestHandler,
    ): PrendaRepositoryRest {
        val cliente = HttpClient(MockEngine { pedido -> pedidos += pedido; manejador(pedido) }) {
            install(ContentNegotiation) { json(jsonFirebase) }
        }
        return PrendaRepositoryRest(cliente, config, tokens, auth, intervaloMs = 60_000L)
    }

    private fun documento(id: String, nombre: String, uid: String = "uid1") = """
        {"name":"projects/proyecto/databases/(default)/documents/prendas/$id","fields":{
          "userId":{"stringValue":"$uid"},"nombre":{"stringValue":"$nombre"},"descripcion":{"stringValue":"d"},
          "categoria":{"stringValue":"superior"},"colores":{"arrayValue":{"values":[{"stringValue":"rojo"}]}},
          "ocasiones":{"arrayValue":{"values":[{"stringValue":"casual"}]}},"fotoUrl":{"stringValue":""}}}
    """.trimIndent()

    private fun HttpRequestData.cuerpo(): JsonObject =
        jsonFirebase.parseToJsonElement((body as TextContent).text).jsonObject

    private val prendaNueva = Prenda(
        id = "", nombre = "Remera", descripcion = "Lisa", categoria = Categoria.SUPERIOR,
        colores = listOf("blanco"), ocasiones = listOf(Ocasion.CASUAL), fotoUrl = "",
    )

    @Test
    fun listarConResultadosArmaLasPrendasYUsaLaConsultaPorUsuario() = pruebaReal {
        val repo = repo {
            respond("""[{"document":${documento("p1", "Remera")},"readTime":"t"},{"document":${documento("p2", "Jean")}}]""", headers = json)
        }

        val prendas = repo.observarPrendas().first()

        assertEquals(listOf("p1", "p2"), prendas.map { it.id })
        assertEquals(listOf("Remera", "Jean"), prendas.map { it.nombre })
        val pedido = pedidos.first()
        assertEquals(HttpMethod.Post, pedido.method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents:runQuery", pedido.url.encodedPath)
        assertEquals("Bearer TOKEN1", pedido.headers[HttpHeaders.Authorization])
        val filtro = pedido.cuerpo()["structuredQuery"]!!.jsonObject["where"]!!.jsonObject["fieldFilter"]!!.jsonObject
        assertEquals("userId", filtro["field"]!!.jsonObject["fieldPath"]!!.jsonPrimitive.content)
        assertEquals("EQUAL", filtro["op"]!!.jsonPrimitive.content)
        assertEquals("uid1", filtro["value"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
    }

    @Test
    fun listarSinResultadosDevuelveListaVacia() = pruebaReal {
        // Cuando no hay documentos, Firestore responde un único elemento sin "document".
        val repo = repo { respond("""[{"readTime":"2026-11-16T10:00:00Z"}]""", headers = json) }

        assertEquals(emptyList(), repo.observarPrendas().first())
    }

    @Test
    fun sinUsuarioNoHaceNingunPedidoYEmiteVacio() = pruebaReal {
        val sinSesion = AuthDePrueba(null)
        val cliente = HttpClient(MockEngine { pedidos += it; respond("[]", headers = json) })
        val repo = PrendaRepositoryRest(cliente, config, tokens, sinSesion, intervaloMs = 60_000L)

        assertEquals(emptyList(), repo.observarPrendas().first())
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun siElPrimerSondeoFallaElFlowInformaElError() = pruebaReal {
        val repo = repo { respond("{}", HttpStatusCode.Forbidden, json) }

        val error = assertFailsWith<DomainException> { repo.observarPrendas().first() }

        assertIs<DomainException.Desconocido>(error)
        assertTrue(error.cause?.message.orEmpty().contains("403"))
    }

    @Test
    fun sinConexionAlListarElErrorEsSinConexion() = pruebaReal {
        val repo = repo { throw IllegalStateException("sin red") }

        assertFailsWith<DomainException.SinConexion> { repo.observarPrendas().first() }
    }

    @Test
    fun conUn401RenuevaElTokenUnaVezYReintenta() = pruebaReal {
        var intentos = 0
        val repo = repo { pedido ->
            intentos++
            if (pedido.headers[HttpHeaders.Authorization] == "Bearer TOKEN1") {
                respond("{}", HttpStatusCode.Unauthorized, json)
            } else {
                respond("""[{"document":${documento("p1", "Remera")}}]""", headers = json)
            }
        }

        val prendas = repo.observarPrendas().first()

        assertEquals(1, prendas.size)
        assertEquals(2, intentos)
        assertEquals(1, tokens.renovaciones)
        assertEquals("Bearer TOKEN2", pedidos.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun siElReintentoTambienDa401ElErrorEsSesionNoIniciada() = pruebaReal {
        val repo = repo { respond("{}", HttpStatusCode.Unauthorized, json) }

        assertFailsWith<DomainException.SesionNoIniciada> { repo.observarPrendas().first() }
        assertEquals(2, pedidos.size)
    }

    @Test
    fun obtenerPrendaDevuelveLaPrendaDelUsuario() = pruebaReal {
        val repo = repo { respond(documento("p1", "Remera"), headers = json) }

        val prenda = repo.obtenerPrenda("p1")

        assertEquals("p1", prenda?.id)
        assertEquals(HttpMethod.Get, pedidos.single().method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents/prendas/p1", pedidos.single().url.encodedPath)
    }

    @Test
    fun obtenerPrendaDeOtroUsuarioDevuelveNull() = pruebaReal {
        val repo = repo { respond(documento("p1", "Ajena", uid = "otro"), headers = json) }

        assertNull(repo.obtenerPrenda("p1"))
    }

    @Test
    fun obtenerPrendaConErrores403Y404DevuelveNull() = pruebaReal {
        listOf(HttpStatusCode.Forbidden, HttpStatusCode.NotFound).forEach { estado ->
            val repo = repo { respond("{}", estado, json) }

            assertNull(repo.obtenerPrenda("p1"), "con $estado")
        }
    }

    @Test
    fun obtenerPrendaConErrorDelServidorLanzaError() = pruebaReal {
        val repo = repo { respond("{}", HttpStatusCode.InternalServerError, json) }

        assertFailsWith<DomainException.Desconocido> { repo.obtenerPrenda("p1") }
    }

    @Test
    fun obtenerPrendaSinSesionDevuelveNull() = pruebaReal {
        tokens.uidActual = null
        val repo = repo { respond(documento("p1", "x"), headers = json) }

        assertNull(repo.obtenerPrenda("p1"))
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun crearUnaPrendaNuevaHaceUnPostConTodosLosCampos() = pruebaReal {
        val repo = repo { respond(documento("nueva", "Remera"), headers = json) }

        val resultado = repo.guardarPrenda(prendaNueva)

        assertTrue(resultado.isSuccess)
        val pedido = pedidos.single()
        assertEquals(HttpMethod.Post, pedido.method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents/prendas", pedido.url.encodedPath)
        val campos = pedido.cuerpo()["fields"]!!.jsonObject
        assertEquals(
            setOf("userId", "nombre", "descripcion", "categoria", "colores", "ocasiones", "fotoUrl"),
            campos.keys,
        )
        assertEquals("uid1", campos["userId"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
        assertEquals("superior", campos["categoria"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
    }

    @Test
    fun editarUnaPrendaHaceUnPatchConUpdateMaskYSinUserId() = pruebaReal {
        val repo = repo { respond(documento("p1", "Remera"), headers = json) }

        val resultado = repo.guardarPrenda(prendaNueva.copy(id = "p1", nombre = "Remera editada"))

        assertTrue(resultado.isSuccess)
        val pedido = pedidos.single()
        assertEquals(HttpMethod.Patch, pedido.method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents/prendas/p1", pedido.url.encodedPath)
        assertEquals(
            CAMPOS_EDITABLES_PRENDA.toSet(),
            pedido.url.parameters.getAll("updateMask.fieldPaths")!!.toSet(),
        )
        val campos = pedido.cuerpo()["fields"]!!.jsonObject
        assertFalse("userId" in campos, "editar no debe tocar el userId")
        assertEquals("Remera editada", campos["nombre"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
    }

    @Test
    fun guardarSinSesionFallaConSesionNoIniciada() = pruebaReal {
        tokens.uidActual = null
        val repo = repo { respond("{}", headers = json) }

        assertSame(DomainException.SesionNoIniciada, repo.guardarPrenda(prendaNueva).exceptionOrNull())
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun guardarConErroresHttpDevuelveElErrorCorrespondiente() = pruebaReal {
        val sinPermiso = repo { respond("{}", HttpStatusCode.Forbidden, json) }
        val noEncontrada = repo { respond("{}", HttpStatusCode.NotFound, json) }
        val caido = repo { respond("{}", HttpStatusCode.ServiceUnavailable, json) }

        assertTrue(sinPermiso.guardarPrenda(prendaNueva).exceptionOrNull()!!.cause!!.message!!.contains("403"))
        assertTrue(noEncontrada.guardarPrenda(prendaNueva.copy(id = "x")).exceptionOrNull()!!.cause!!.message!!.contains("404"))
        assertTrue(caido.guardarPrenda(prendaNueva).exceptionOrNull()!!.cause!!.message!!.contains("503"))
    }

    @Test
    fun guardarSinConexionDevuelveSinConexion() = pruebaReal {
        val repo = repo { throw IllegalStateException("sin red") }

        assertSame(DomainException.SinConexion, repo.guardarPrenda(prendaNueva).exceptionOrNull())
    }

    @Test
    fun guardarConUn401RenuevaElTokenYReintenta() = pruebaReal {
        val repo = repo { pedido ->
            if (pedido.headers[HttpHeaders.Authorization] == "Bearer TOKEN1") {
                respond("{}", HttpStatusCode.Unauthorized, json)
            } else {
                respond(documento("p1", "x"), headers = json)
            }
        }

        assertTrue(repo.guardarPrenda(prendaNueva.copy(id = "p1")).isSuccess)
        assertEquals(1, tokens.renovaciones)
    }

    @Test
    fun eliminarHaceUnDeleteYAceptaLaRespuestaVacia() = pruebaReal {
        val repo = repo { respond("{}", HttpStatusCode.OK, json) }

        assertTrue(repo.eliminarPrenda("p1").isSuccess)
        assertEquals(HttpMethod.Delete, pedidos.single().method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents/prendas/p1", pedidos.single().url.encodedPath)
    }

    @Test
    fun eliminarConErroresHttpYSinSesionFalla() = pruebaReal {
        val prohibido = repo { respond("{}", HttpStatusCode.Forbidden, json) }
        assertTrue(prohibido.eliminarPrenda("p1").isFailure)

        tokens.uidActual = null
        assertSame(DomainException.SesionNoIniciada, prohibido.eliminarPrenda("p1").exceptionOrNull())
    }

    @Test
    fun sinProjectIdTodoFallaClaroSinHacerPedidos() = pruebaReal {
        val repo = repo(config = ConfigFirebaseEscritorio(apiKey = "clave", projectId = "")) { respond("[]", headers = json) }

        assertTrue(repo.guardarPrenda(prendaNueva).isFailure)
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun despuesDeGuardarLaListaSeRefrescaSinEsperarAlSondeo() = pruebaReal {
        var guardada = false
        val repo = repo { pedido ->
            when {
                pedido.url.encodedPath.endsWith(":runQuery") ->
                    respond(if (guardada) "[{\"document\":${documento("p1", "Remera")}}]" else "[{}]", headers = json)
                else -> {
                    guardada = true
                    respond(documento("p1", "Remera"), headers = json)
                }
            }
        }
        val emisiones = mutableListOf<List<Prenda>>()
        val primera = CompletableDeferred<Unit>()
        val colector = launch {
            repo.observarPrendas().take(2).collect {
                emisiones += it
                primera.complete(Unit)
            }
        }
        primera.await()

        repo.guardarPrenda(prendaNueva)
        withTimeout(10_000) { colector.join() }

        assertEquals(emptyList(), emisiones[0])
        assertEquals(listOf("p1"), emisiones[1].map { it.id })
    }
}
