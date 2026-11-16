package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.config.ConfigFirebaseEscritorio
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Usuario
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AuthRepositoryRestTest {

    private val config = ConfigFirebaseEscritorio(apiKey = "clave-de-prueba", projectId = "proyecto")
    private var ahoraMs = 1_000_000L
    private val almacen = AlmacenSesionEnMemoria()
    private val pedidos = mutableListOf<HttpRequestData>()

    private val sesionOk = """
        {"idToken":"T1","refreshToken":"R1","expiresIn":"3600","localId":"uid1","email":"ana@test.com"}
    """.trimIndent()

    private fun errorFirebase(mensaje: String) = """{"error":{"code":400,"message":"$mensaje"}}"""

    private fun repo(
        almacen: AlmacenSesion = this.almacen,
        config: ConfigFirebaseEscritorio = this.config,
        manejador: MockRequestHandler,
    ): AuthRepositoryRest {
        val cliente = HttpClient(
            MockEngine { pedido ->
                pedidos += pedido
                manejador(pedido)
            },
        ) {
            install(ContentNegotiation) { json(jsonFirebase) }
        }
        return AuthRepositoryRest(cliente, config, almacen, ahora = { ahoraMs })
    }

    private fun HttpRequestData.cuerpoTexto(): String = (body as? TextContent)?.text.orEmpty()

    private fun HttpRequestData.formulario() = (body as FormDataContent).formData

    @Test
    fun loginCorrectoDevuelveElUsuarioYGuardaLaSesion() = runTest {
        val repo = repo { respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json")) }

        val resultado = repo.login("ana@test.com", "123456")

        assertEquals(Usuario(id = "uid1", email = "ana@test.com"), resultado.getOrNull())
        assertEquals(Usuario(id = "uid1", email = "ana@test.com"), repo.usuarioActual.first())
        assertEquals("T1", almacen.leer()?.idToken)
        assertEquals("R1", almacen.leer()?.refreshToken)
    }

    @Test
    fun loginPideLaUrlCorrectaConLaClaveYLasCredenciales() = runTest {
        val repo = repo { respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json")) }

        repo.login("ana@test.com", "123456")

        val pedido = pedidos.single()
        assertEquals(HttpMethod.Post, pedido.method)
        assertEquals("identitytoolkit.googleapis.com", pedido.url.host)
        assertEquals("/v1/accounts:signInWithPassword", pedido.url.encodedPath)
        assertEquals("clave-de-prueba", pedido.url.parameters["key"])
        val cuerpo = pedido.cuerpoTexto()
        assertTrue(cuerpo.contains("\"email\":\"ana@test.com\""), cuerpo)
        assertTrue(cuerpo.contains("\"password\":\"123456\""), cuerpo)
        assertTrue(cuerpo.contains("\"returnSecureToken\":true"), cuerpo)
    }

    @Test
    fun laContrasenaNuncaSeGuardaEnLaSesion() = runTest {
        val repo = repo { respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json")) }

        repo.login("ana@test.com", "mi-contrasena-secreta")

        assertFalse(almacen.leer().toString().contains("mi-contrasena-secreta"))
    }

    @Test
    fun registroCorrectoUsaElEndpointSignUp() = runTest {
        val repo = repo { respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json")) }

        val resultado = repo.registrar("ana@test.com", "123456")

        assertTrue(resultado.isSuccess)
        assertEquals("/v1/accounts:signUp", pedidos.single().url.encodedPath)
        assertEquals("uid1", repo.usuarioActual.first()?.id)
    }

    @Test
    fun loginMapeaCadaErrorDeFirebaseAlErrorDeDominio() = runTest {
        val casos = listOf(
            "EMAIL_NOT_FOUND" to DomainException.CredencialesInvalidas,
            "INVALID_PASSWORD" to DomainException.CredencialesInvalidas,
            "INVALID_LOGIN_CREDENTIALS" to DomainException.CredencialesInvalidas,
            "USER_DISABLED" to DomainException.CredencialesInvalidas,
            "INVALID_EMAIL" to DomainException.EmailInvalido,
        )
        casos.forEach { (mensaje, esperado) ->
            val repo = repo {
                respond(errorFirebase(mensaje), HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/json"))
            }

            val error = repo.login("ana@test.com", "123456").exceptionOrNull()

            assertSame(esperado, error, "con $mensaje")
        }
    }

    @Test
    fun registroMapeaEmailExistenteYContrasenaDebilIncluidoElDetalle() = runTest {
        val existente = repo {
            respond(errorFirebase("EMAIL_EXISTS"), HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val debil = repo {
            respond(
                errorFirebase("WEAK_PASSWORD : Password should be at least 6 characters"),
                HttpStatusCode.BadRequest,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertSame(DomainException.EmailYaRegistrado, existente.registrar("a@b.com", "123456").exceptionOrNull())
        assertSame(DomainException.ContrasenaDebil, debil.registrar("a@b.com", "123").exceptionOrNull())
    }

    @Test
    fun demasiadosIntentosYErroresDesconocidosSonDesconocido() = runTest {
        listOf("TOO_MANY_ATTEMPTS_TRY_LATER", "ALGO_RARO").forEach { mensaje ->
            val repo = repo {
                respond(errorFirebase(mensaje), HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/json"))
            }

            assertIs<DomainException.Desconocido>(repo.login("a@b.com", "123456").exceptionOrNull(), "con $mensaje")
        }
    }

    @Test
    fun siLaRespuestaDeErrorNoEsJsonIgualDevuelveDesconocido() = runTest {
        val repo = repo { respond("<html>Bad gateway</html>", HttpStatusCode.BadGateway) }

        assertIs<DomainException.Desconocido>(repo.login("a@b.com", "123456").exceptionOrNull())
    }

    @Test
    fun sinConexionDevuelveSinConexionYNoGuardaNada() = runTest {
        val repo = repo { throw IllegalStateException("sin red") }

        assertSame(DomainException.SinConexion, repo.login("a@b.com", "123456").exceptionOrNull())
        assertNull(almacen.leer())
        assertNull(repo.usuarioActual.first())
    }

    @Test
    fun sinLaClaveDeFirebaseFallaClaroYSinHacerPedidos() = runTest {
        val repo = repo(config = ConfigFirebaseEscritorio(apiKey = "", projectId = "proyecto")) {
            respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val error = repo.login("a@b.com", "123456").exceptionOrNull()

        assertIs<DomainException.Desconocido>(error)
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun unaRespuestaSinTokenNoIniciaSesion() = runTest {
        val repo = repo { respond("""{"email":"a@b.com"}""", headers = headersOf(HttpHeaders.ContentType, "application/json")) }

        assertIs<DomainException.Desconocido>(repo.login("a@b.com", "123456").exceptionOrNull())
        assertNull(repo.usuarioActual.first())
    }

    @Test
    fun alReabrirLaAppLaSesionGuardadaSigueIniciada() = runTest {
        val primera = repo { respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
        primera.login("ana@test.com", "123456")

        // Es como cerrar y volver a abrir la app: un repositorio nuevo sobre el mismo almacén.
        val reabierta = repo { throw IllegalStateException("no debería hacer pedidos") }

        assertEquals(Usuario("uid1", "ana@test.com"), reabierta.usuarioActual.first())
        assertEquals("uid1", reabierta.uid)
    }

    @Test
    fun cerrarSesionBorraTodoYEmiteNull() = runTest {
        val repo = repo { respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
        repo.login("ana@test.com", "123456")

        repo.cerrarSesion()

        assertNull(repo.usuarioActual.first())
        assertNull(almacen.leer())
        assertNull(repo.uid)
    }

    @Test
    fun conElTokenVigenteNoSeHaceNingunPedidoNuevo() = runTest {
        val repo = repo { respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json")) }
        repo.login("ana@test.com", "123456")

        val token = repo.idTokenVigente()

        assertEquals("T1", token.getOrNull())
        assertEquals(1, pedidos.size)
    }

    @Test
    fun siElTokenVencioLoRenuevaConElRefreshToken() = runTest {
        val repo = repo { pedido ->
            if (pedido.url.host == "securetoken.googleapis.com") {
                respond(
                    """{"id_token":"T2","refresh_token":"R2","expires_in":"3600","user_id":"uid1"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        repo.login("ana@test.com", "123456")
        ahoraMs += 3_600_000L // pasó una hora: el token venció

        val token = repo.idTokenVigente()

        assertEquals("T2", token.getOrNull())
        val renovacion = pedidos.last()
        assertEquals("/v1/token", renovacion.url.encodedPath)
        assertEquals("clave-de-prueba", renovacion.url.parameters["key"])
        assertEquals("refresh_token", renovacion.formulario()["grant_type"])
        assertEquals("R1", renovacion.formulario()["refresh_token"])
        assertEquals("T2", almacen.leer()?.idToken)
        assertEquals("R2", almacen.leer()?.refreshToken)
    }

    @Test
    fun conForzarRenuevaAunqueElTokenSigaVigente() = runTest {
        val repo = repo { pedido ->
            if (pedido.url.host == "securetoken.googleapis.com") {
                respond("""{"id_token":"T9","refresh_token":"R9","expires_in":"3600"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        repo.login("ana@test.com", "123456")

        val token = repo.idTokenVigente(forzar = true)

        assertEquals("T9", token.getOrNull())
        assertEquals(2, pedidos.size)
    }

    @Test
    fun siFirebaseYaNoAceptaElRefreshTokenSeCierraLaSesion() = runTest {
        val repo = repo { pedido ->
            if (pedido.url.host == "securetoken.googleapis.com") {
                respond(errorFirebase("TOKEN_EXPIRED"), HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        repo.login("ana@test.com", "123456")
        ahoraMs += 3_600_000L

        val token = repo.idTokenVigente()

        assertSame(DomainException.SesionNoIniciada, token.exceptionOrNull())
        assertNull(repo.usuarioActual.first(), "la app tiene que volver al login")
        assertNull(almacen.leer())
    }

    @Test
    fun sinConexionAlRenovarNoSePierdeLaSesion() = runTest {
        val repo = repo { pedido ->
            if (pedido.url.host == "securetoken.googleapis.com") {
                throw IllegalStateException("sin red")
            } else {
                respond(sesionOk, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        repo.login("ana@test.com", "123456")
        ahoraMs += 3_600_000L

        val token = repo.idTokenVigente()

        assertSame(DomainException.SinConexion, token.exceptionOrNull())
        assertEquals("uid1", repo.usuarioActual.first()?.id)
        assertEquals("R1", almacen.leer()?.refreshToken)
    }

    @Test
    fun sinSesionNoHayTokenVigente() = runTest {
        val repo = repo { respond(sesionOk) }

        assertSame(DomainException.SesionNoIniciada, repo.idTokenVigente().exceptionOrNull())
        assertTrue(pedidos.isEmpty())
    }

    @Test
    fun elCodigoDeErrorSeSeparaDelDetalle() {
        assertEquals("WEAK_PASSWORD", codigoDeErrorFirebase("WEAK_PASSWORD : Password should be at least 6 characters"))
        assertEquals("EMAIL_EXISTS", codigoDeErrorFirebase("EMAIL_EXISTS"))
        assertEquals("", codigoDeErrorFirebase(null))
    }
}
