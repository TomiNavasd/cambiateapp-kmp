package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.config.ConfigFirebaseEscritorio
import com.example.cambiateappkmp.data.mapper.CAMPOS_EDITABLES_OUTFIT
import com.example.cambiateappkmp.domain.error.CampoInvalido
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Usuario
import com.example.cambiateappkmp.domain.repository.AuthRepository
import com.example.cambiateappkmp.domain.usecase.SaveOutfitUseCase
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
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** runTest salta el tiempo virtual mientras espera al motor HTTP; con Default los timeouts corren en tiempo real. */
private fun pruebaDeOutfits(bloque: suspend CoroutineScope.() -> Unit) =
    runTest { withContext(Dispatchers.Default) { bloque() } }

private class TokensDeOutfits(var uidActual: String? = "uid1") : TokenProvider {
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

private class AuthDeOutfits(usuario: Usuario?) : AuthRepository {
    val flujo = MutableStateFlow(usuario)
    override val usuarioActual: Flow<Usuario?> = flujo
    override suspend fun login(email: String, pass: String) = Result.failure<Usuario>(UnsupportedOperationException())
    override suspend fun registrar(email: String, pass: String) = Result.failure<Usuario>(UnsupportedOperationException())
    override suspend fun cerrarSesion() { flujo.value = null }
}

class OutfitsRepositoryRestTest {

    private val config = ConfigFirebaseEscritorio(apiKey = "clave", projectId = "proyecto")
    private val tokens = TokensDeOutfits()
    private val auth = AuthDeOutfits(Usuario("uid1", "ana@test.com"))
    private val pedidos = mutableListOf<HttpRequestData>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repo(
        config: ConfigFirebaseEscritorio = this.config,
        manejador: MockRequestHandler,
    ): OutfitsRepositoryRest {
        val cliente = HttpClient(MockEngine { pedido -> pedidos += pedido; manejador(pedido) }) {
            install(ContentNegotiation) { json(jsonFirebase) }
        }
        return OutfitsRepositoryRest(cliente, config, tokens, auth, intervaloMs = 60_000L)
    }

    private fun documento(
        id: String,
        ocasion: String = "casual",
        fecha: String = "2026-11-16T15:45:00Z",
        uid: String = "uid1",
        favorito: Boolean = false,
    ) = """
        {"name":"projects/proyecto/databases/(default)/documents/outfits/$id","fields":{
          "userId":{"stringValue":"$uid"},"ocasion":{"stringValue":"$ocasion"},
          "prendasIds":{"arrayValue":{"values":[{"stringValue":"p1"},{"stringValue":"p2"}]}},
          "fecha":{"timestampValue":"$fecha"},"favorito":{"booleanValue":$favorito}}}
    """.trimIndent()

    private fun HttpRequestData.cuerpo(): JsonObject =
        jsonFirebase.parseToJsonElement((body as TextContent).text).jsonObject

    private fun HttpRequestData.mascara(): Set<String> = url.parameters.getAll("updateMask.fieldPaths").orEmpty().toSet()

    private val fecha = Instant.parse("2026-11-16T15:45:00Z")
    private val nuevo = Outfit("", Ocasion.SALIDA, listOf("p1", "p2", "p3"), fecha, favorito = false)
    private val existente = Outfit("o1", Ocasion.TRABAJO, listOf("p9"), fecha, favorito = true)

    private val soloDeLaNativaONoEditable = listOf("userId", "fecha")

    // ---- listar y observar ----

    @Test
    fun listarArmaLosOutfitsOrdenadosDelMasNuevoAlMasViejoYUsaLaConsultaPorUsuario() = pruebaDeOutfits {
        val repo = repo {
            respond(
                "[" +
                    """{"document":${documento("viejo", fecha = "2026-01-01T10:00:00Z")},"readTime":"t"},""" +
                    """{"document":${documento("nuevo", fecha = "2026-11-20T10:00:00Z")}},""" +
                    """{"document":${documento("medio", fecha = "2026-06-15T10:00:00.5Z")}}]""",
                headers = json,
            )
        }

        val outfits = repo.listarOutfits().getOrThrow()

        assertEquals(listOf("nuevo", "medio", "viejo"), outfits.map { it.id })
        assertEquals(listOf(Ocasion.CASUAL), outfits.map { it.ocasion }.distinct())
        assertEquals(listOf("p1", "p2"), outfits.first().prendasIds)
        val pedido = pedidos.single()
        assertEquals(HttpMethod.Post, pedido.method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents:runQuery", pedido.url.encodedPath)
        assertEquals("Bearer TOKEN1", pedido.headers[HttpHeaders.Authorization])
        val consulta = pedido.cuerpo()["structuredQuery"]!!.jsonObject
        assertEquals("outfits", consulta["from"]!!.jsonArray.single().jsonObject["collectionId"]!!.jsonPrimitive.content)
        val filtro = consulta["where"]!!.jsonObject["fieldFilter"]!!.jsonObject
        assertEquals("userId", filtro["field"]!!.jsonObject["fieldPath"]!!.jsonPrimitive.content)
        assertEquals("EQUAL", filtro["op"]!!.jsonPrimitive.content)
        assertEquals("uid1", filtro["value"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
        assertFalse("orderBy" in consulta, "el orden se hace en memoria, sin índice")
    }

    @Test
    fun listarSinResultadosDevuelveListaVacia() = pruebaDeOutfits {
        // Cuando no hay documentos, Firestore responde un único elemento sin "document".
        val repo = repo { respond("""[{"readTime":"2026-11-16T10:00:00Z"}]""", headers = json) }

        assertEquals(emptyList(), repo.listarOutfits().getOrThrow())
    }

    @Test
    fun unaOcasionDesconocidaSeDescartaYElRestoSeConserva() = pruebaDeOutfits {
        val repo = repo {
            respond(
                """[{"document":${documento("a", ocasion = "fiesta")}},{"document":${documento("b", ocasion = "salida")}}]""",
                headers = json,
            )
        }

        val outfits = repo.listarOutfits().getOrThrow()

        assertEquals(listOf("b"), outfits.map { it.id })
        assertEquals(Ocasion.SALIDA, outfits.single().ocasion)
    }

    @Test
    fun laFechaDelDocumentoLlegaComoInstant() = pruebaDeOutfits {
        val repo = repo { respond("""[{"document":${documento("a", fecha = "2026-11-16T15:45:00.250Z", favorito = true)}}]""", headers = json) }

        val outfit = repo.listarOutfits().getOrThrow().single()

        assertEquals(Instant.parse("2026-11-16T15:45:00.250Z"), outfit.fecha)
        assertEquals(true, outfit.favorito)
    }

    @Test
    fun observarEmiteLosOutfitsOrdenados() = pruebaDeOutfits {
        val repo = repo {
            respond(
                """[{"document":${documento("a", fecha = "2026-01-01T00:00:00Z")}},{"document":${documento("b", fecha = "2026-02-01T00:00:00Z")}}]""",
                headers = json,
            )
        }

        assertEquals(listOf("b", "a"), repo.observarOutfits().first().map { it.id })
    }

    @Test
    fun sinUsuarioObservarEmiteVacioYNoHaceNingunPedido() = pruebaDeOutfits {
        val sinSesion = AuthDeOutfits(null)
        val cliente = HttpClient(MockEngine { pedidos += it; respond("[]", headers = json) })
        val repo = OutfitsRepositoryRest(cliente, config, tokens, sinSesion, intervaloMs = 60_000L)

        assertEquals(emptyList(), repo.observarOutfits().first())
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun listarSinSesionFallaConSesionNoIniciada() = pruebaDeOutfits {
        tokens.uidActual = null
        val repo = repo { respond("[]", headers = json) }

        assertSame(DomainException.SesionNoIniciada, repo.listarOutfits().exceptionOrNull())
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun despuesDeGuardarLaListaSeRefrescaSinEsperarAlSondeo() = pruebaDeOutfits {
        var guardado = false
        val repo = repo { pedido ->
            when {
                pedido.url.encodedPath.endsWith(":runQuery") ->
                    respond(if (guardado) """[{"document":${documento("o1")}}]""" else "[{}]", headers = json)
                else -> {
                    guardado = true
                    respond(documento("o1"), headers = json)
                }
            }
        }
        val emisiones = mutableListOf<List<Outfit>>()
        val primera = CompletableDeferred<Unit>()
        val colector = launch {
            repo.observarOutfits().take(2).collect {
                emisiones += it
                primera.complete(Unit)
            }
        }
        primera.await()

        repo.guardarOutfit(nuevo)
        withTimeout(10_000) { colector.join() }

        assertEquals(emptyList(), emisiones[0])
        assertEquals(listOf("o1"), emisiones[1].map { it.id })
    }

    // ---- errores al leer ----

    @Test
    fun los403AlListarSonFaltaDePermisosYLosErroresDeServidorSonDesconocidos() = pruebaDeOutfits {
        assertSame(DomainException.SinPermisos, repo { respond("{}", HttpStatusCode.Forbidden, json) }.listarOutfits().exceptionOrNull())

        val error = repo { respond("{}", HttpStatusCode.InternalServerError, json) }.listarOutfits().exceptionOrNull()
        assertIs<DomainException.Desconocido>(error)
        assertTrue(error.cause?.message.orEmpty().contains("500"))
    }

    @Test
    fun siElPrimerSondeoFallaElFlowInformaElError() = pruebaDeOutfits {
        val repo = repo { respond("{}", HttpStatusCode.Forbidden, json) }

        assertFailsWith<DomainException.SinPermisos> { repo.observarOutfits().first() }
    }

    @Test
    fun sinConexionAlListarElErrorEsSinConexion() = pruebaDeOutfits {
        val repo = repo { throw IllegalStateException("sin red") }

        assertSame(DomainException.SinConexion, repo.listarOutfits().exceptionOrNull())
        assertFailsWith<DomainException.SinConexion> { repo.observarOutfits().first() }
    }

    @Test
    fun unaRespuestaQueNoEsJsonEsUnErrorDesconocido() = pruebaDeOutfits {
        val repo = repo { respond("<html>", headers = json) }

        assertIs<DomainException.Desconocido>(repo.listarOutfits().exceptionOrNull())
    }

    @Test
    fun conUn401RenuevaElTokenUnaVezYReintenta() = pruebaDeOutfits {
        var intentos = 0
        val repo = repo { pedido ->
            intentos++
            if (pedido.headers[HttpHeaders.Authorization] == "Bearer TOKEN1") {
                respond("{}", HttpStatusCode.Unauthorized, json)
            } else {
                respond("""[{"document":${documento("o1")}}]""", headers = json)
            }
        }

        val outfits = repo.listarOutfits().getOrThrow()

        assertEquals(1, outfits.size)
        assertEquals(2, intentos)
        assertEquals(1, tokens.renovaciones)
        assertEquals("Bearer TOKEN2", pedidos.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun siElReintentoTambienDa401ElErrorEsSesionNoIniciada() = pruebaDeOutfits {
        val repo = repo { respond("{}", HttpStatusCode.Unauthorized, json) }

        assertSame(DomainException.SesionNoIniciada, repo.listarOutfits().exceptionOrNull())
        assertEquals(2, pedidos.size)
    }

    // ---- guardar ----

    @Test
    fun crearUnOutfitNuevoHaceUnPostConTodosLosCamposIncluidoElUserId() = pruebaDeOutfits {
        val repo = repo { respond(documento("nuevo"), headers = json) }

        assertTrue(repo.guardarOutfit(nuevo).isSuccess)

        val pedido = pedidos.single()
        assertEquals(HttpMethod.Post, pedido.method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents/outfits", pedido.url.encodedPath)
        assertTrue(pedido.url.parameters.isEmpty(), "sin updateMask: es un documento nuevo")
        val campos = pedido.cuerpo()["fields"]!!.jsonObject
        assertEquals(setOf("userId", "ocasion", "prendasIds", "fecha", "favorito"), campos.keys)
        assertEquals("uid1", campos["userId"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
        assertEquals("salida", campos["ocasion"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
        assertEquals("2026-11-16T15:45:00Z", campos["fecha"]!!.jsonObject["timestampValue"]!!.jsonPrimitive.content)
        assertEquals("false", campos["favorito"]!!.jsonObject["booleanValue"]!!.jsonPrimitive.content)
        assertEquals(
            listOf("p1", "p2", "p3"),
            campos["prendasIds"]!!.jsonObject["arrayValue"]!!.jsonObject["values"]!!.jsonArray.map {
                it.jsonObject["stringValue"]!!.jsonPrimitive.content
            },
        )
    }

    @Test
    fun editarUnOutfitHaceUnPatchConUpdateMaskDeSoloOcasionPrendasYFavorito() = pruebaDeOutfits {
        val repo = repo { respond(documento("o1"), headers = json) }

        assertTrue(repo.guardarOutfit(existente).isSuccess)

        val pedido = pedidos.single()
        assertEquals(HttpMethod.Patch, pedido.method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents/outfits/o1", pedido.url.encodedPath)
        val campos = pedido.cuerpo()["fields"]!!.jsonObject
        val esperados = setOf("ocasion", "prendasIds", "favorito")
        // El cuerpo y la máscara llevan exactamente esos tres campos: lo demás queda como está en Firestore.
        assertEquals(esperados, CAMPOS_EDITABLES_OUTFIT.toSet())
        assertEquals(esperados, campos.keys)
        assertEquals(esperados, pedido.mascara())
        soloDeLaNativaONoEditable.forEach {
            assertFalse(it in campos, it)
            assertFalse(it in pedido.mascara(), it)
        }
        assertEquals("trabajo", campos["ocasion"]!!.jsonObject["stringValue"]!!.jsonPrimitive.content)
        assertEquals("true", campos["favorito"]!!.jsonObject["booleanValue"]!!.jsonPrimitive.content)
    }

    @Test
    fun cambiarFavoritoEscribeSoloEseCampoYExigeQueElOutfitExista() = pruebaDeOutfits {
        val repo = repo { respond(documento("o1", favorito = true), headers = json) }

        assertTrue(repo.cambiarFavorito("o1", true).isSuccess)

        val pedido = pedidos.single()
        assertEquals(HttpMethod.Patch, pedido.method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents/outfits/o1", pedido.url.encodedPath)
        assertEquals(setOf("favorito"), pedido.mascara())
        assertEquals("true", pedido.url.parameters["currentDocument.exists"])
        val campos = pedido.cuerpo()["fields"]!!.jsonObject
        assertEquals(setOf("favorito"), campos.keys)
        assertEquals("true", campos["favorito"]!!.jsonObject["booleanValue"]!!.jsonPrimitive.content)
    }

    @Test
    fun desmarcarFavoritoMandaFalse() = pruebaDeOutfits {
        val repo = repo { respond(documento("o1"), headers = json) }

        repo.cambiarFavorito("o1", false)

        assertEquals(
            "false",
            pedidos.single().cuerpo()["fields"]!!.jsonObject["favorito"]!!.jsonObject["booleanValue"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun cambiarFavoritoDeUnOutfitQueNoExisteEsUnError404() = pruebaDeOutfits {
        val repo = repo { respond("{}", HttpStatusCode.NotFound, json) }

        val error = repo.cambiarFavorito("fantasma", true).exceptionOrNull()

        assertIs<DomainException.Desconocido>(error)
        assertTrue(error.cause?.message.orEmpty().contains("404"))
    }

    @Test
    fun eliminarHaceUnDeleteYAceptaLaRespuestaVacia() = pruebaDeOutfits {
        val repo = repo { respond("{}", HttpStatusCode.OK, json) }

        assertTrue(repo.eliminarOutfit("o1").isSuccess)

        assertEquals(HttpMethod.Delete, pedidos.single().method)
        assertEquals("/v1/projects/proyecto/databases/(default)/documents/outfits/o1", pedidos.single().url.encodedPath)
    }

    @Test
    fun losErroresHttpAlEscribirSeMapeanComoEnPrendas() = pruebaDeOutfits {
        val sinPermiso = repo { respond("{}", HttpStatusCode.Forbidden, json) }
        val noEncontrado = repo { respond("{}", HttpStatusCode.NotFound, json) }
        val caido = repo { respond("{}", HttpStatusCode.ServiceUnavailable, json) }

        assertSame(DomainException.SinPermisos, sinPermiso.guardarOutfit(nuevo).exceptionOrNull())
        assertSame(DomainException.SinPermisos, sinPermiso.eliminarOutfit("o1").exceptionOrNull())
        assertTrue(noEncontrado.guardarOutfit(existente).exceptionOrNull()!!.cause!!.message!!.contains("404"))
        assertTrue(noEncontrado.eliminarOutfit("o1").exceptionOrNull()!!.cause!!.message!!.contains("404"))
        assertTrue(caido.guardarOutfit(nuevo).exceptionOrNull()!!.cause!!.message!!.contains("503"))
    }

    @Test
    fun escribirSinConexionDevuelveSinConexion() = pruebaDeOutfits {
        val repo = repo { throw IllegalStateException("sin red") }

        assertSame(DomainException.SinConexion, repo.guardarOutfit(nuevo).exceptionOrNull())
        assertSame(DomainException.SinConexion, repo.eliminarOutfit("o1").exceptionOrNull())
        assertSame(DomainException.SinConexion, repo.cambiarFavorito("o1", true).exceptionOrNull())
    }

    @Test
    fun escribirSinSesionFallaConSesionNoIniciadaSinHacerPedidos() = pruebaDeOutfits {
        tokens.uidActual = null
        val repo = repo { respond("{}", headers = json) }

        assertSame(DomainException.SesionNoIniciada, repo.guardarOutfit(nuevo).exceptionOrNull())
        assertSame(DomainException.SesionNoIniciada, repo.eliminarOutfit("o1").exceptionOrNull())
        assertSame(DomainException.SesionNoIniciada, repo.cambiarFavorito("o1", true).exceptionOrNull())
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun escribirConUn401RenuevaElTokenYReintenta() = pruebaDeOutfits {
        val repo = repo { pedido ->
            if (pedido.headers[HttpHeaders.Authorization] == "Bearer TOKEN1") {
                respond("{}", HttpStatusCode.Unauthorized, json)
            } else {
                respond(documento("o1"), headers = json)
            }
        }

        assertTrue(repo.guardarOutfit(existente).isSuccess)
        assertEquals(1, tokens.renovaciones)
        assertEquals(listOf("Bearer TOKEN1", "Bearer TOKEN2"), pedidos.map { it.headers[HttpHeaders.Authorization] })
    }

    @Test
    fun sinProjectIdTodoFallaClaroSinHacerPedidos() = pruebaDeOutfits {
        val repo = repo(config = ConfigFirebaseEscritorio(apiKey = "clave", projectId = "")) { respond("[]", headers = json) }

        assertTrue(repo.guardarOutfit(nuevo).isFailure)
        assertTrue(repo.listarOutfits().isFailure)
        assertTrue(pedidos.isEmpty())
    }

    // ---- el caso de uso de guardar sigue funcionando sin cambios ----

    @Test
    fun elCasoDeUsoNoGuardaUnOutfitSinPrendasNiHaceNingunPedido() = pruebaDeOutfits {
        val repo = repo { respond("[]", headers = json) }

        val error = SaveOutfitUseCase(repo)(nuevo.copy(prendasIds = emptyList())).exceptionOrNull()

        assertIs<DomainException.Validacion>(error)
        assertEquals(CampoInvalido.PRENDAS, error.campo)
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun elCasoDeUsoDetectaUnDuplicadoLeyendoLosGuardadosYNoEscribe() = pruebaDeOutfits {
        // Ya hay un outfit casual con p1 y p2 (el documento de ejemplo): el mismo conjunto en otro orden es el mismo outfit.
        val repo = repo { respond("""[{"document":${documento("o1", ocasion = "casual")}}]""", headers = json) }

        val resultado = SaveOutfitUseCase(repo)(Outfit("", Ocasion.CASUAL, listOf("p2", "p1"), fecha, false))

        assertSame(DomainException.OutfitYaGuardado, resultado.exceptionOrNull())
        assertEquals(1, pedidos.size, "solo la consulta; no se crea nada")
        assertTrue(pedidos.single().url.encodedPath.endsWith(":runQuery"))
    }

    @Test
    fun elCasoDeUsoGuardaUnOutfitNuevoDespuesDeConsultarLosExistentes() = pruebaDeOutfits {
        val repo = repo { pedido ->
            if (pedido.url.encodedPath.endsWith(":runQuery")) {
                respond("""[{"document":${documento("o1", ocasion = "casual")}}]""", headers = json)
            } else {
                respond(documento("nuevo"), headers = json)
            }
        }

        val resultado = SaveOutfitUseCase(repo)(Outfit("", Ocasion.TRABAJO, listOf("p1", "p2"), fecha, false))

        assertTrue(resultado.isSuccess)
        assertEquals(listOf(HttpMethod.Post, HttpMethod.Post), pedidos.map { it.method })
        assertTrue(pedidos[0].url.encodedPath.endsWith(":runQuery"))
        assertEquals("/v1/projects/proyecto/databases/(default)/documents/outfits", pedidos[1].url.encodedPath)
    }
}
