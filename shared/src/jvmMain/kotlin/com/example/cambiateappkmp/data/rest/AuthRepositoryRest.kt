package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.config.ConfigFirebaseEscritorio
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Usuario
import com.example.cambiateappkmp.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val URL_INICIAR_SESION = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"
private const val URL_REGISTRAR = "https://identitytoolkit.googleapis.com/v1/accounts:signUp"
private const val URL_RENOVAR_TOKEN = "https://securetoken.googleapis.com/v1/token"

/** Margen para renovar el token un poco antes de que venza. */
private const val MARGEN_VENCIMIENTO_SEGUNDOS = 60L

/** Si la renovación falla con alguno de estos códigos, Firebase ya no acepta la sesión guardada. */
private val CODIGOS_SESION_INVALIDA = setOf(
    "TOKEN_EXPIRED",
    "INVALID_REFRESH_TOKEN",
    "USER_DISABLED",
    "USER_NOT_FOUND",
    "MISSING_REFRESH_TOKEN",
)

/** Autenticación de escritorio: habla con la API REST de Firebase Auth (GitLive no funciona bien en JVM). */
internal class AuthRepositoryRest(
    private val cliente: HttpClient,
    private val config: ConfigFirebaseEscritorio,
    private val almacen: AlmacenSesion,
    private val ahora: () -> Long = { System.currentTimeMillis() },
) : AuthRepository, TokenProvider {

    // Si había una sesión guardada, la app arranca con el usuario ya iniciado.
    private val usuario = MutableStateFlow(almacen.leer()?.aUsuario())

    // Evita renovar el token dos veces a la vez.
    private val exclusion = Mutex()

    override val usuarioActual: Flow<Usuario?> = usuario.asStateFlow()

    override val uid: String? get() = usuario.value?.id

    override suspend fun login(email: String, pass: String): Result<Usuario> =
        autenticar(URL_INICIAR_SESION, email, pass)

    override suspend fun registrar(email: String, pass: String): Result<Usuario> =
        autenticar(URL_REGISTRAR, email, pass)

    override suspend fun cerrarSesion() {
        almacen.borrar()
        usuario.value = null
    }

    override suspend fun idTokenVigente(forzar: Boolean): Result<String> = exclusion.withLock {
        val sesion = almacen.leer() ?: return@withLock Result.failure(DomainException.SesionNoIniciada)
        if (!forzar && ahora() < sesion.expiraEnMs && sesion.idToken.isNotBlank()) {
            return@withLock Result.success(sesion.idToken)
        }
        renovar(sesion)
    }

    private suspend fun autenticar(url: String, email: String, pass: String): Result<Usuario> {
        if (config.apiKey.isBlank()) return Result.failure(faltaConfiguracion())

        val respuesta = try {
            cliente.post(url) {
                parameter("key", config.apiKey)
                contentType(ContentType.Application.Json)
                setBody(CredencialesRequest(email = email, password = pass))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Si ni siquiera llegó una respuesta, lo tratamos como falta de conexión.
            // No se guarda la causa: el mensaje de la excepción puede traer la URL con la clave.
            return Result.failure(DomainException.SinConexion)
        }

        if (!respuesta.status.isSuccess()) {
            return Result.failure(mapearErrorAuthRest(respuesta.mensajeDeErrorFirebase(), respuesta.status.value))
        }

        val datos = try {
            respuesta.body<AuthRespuesta>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(DomainException.Desconocido(e))
        }

        if (datos.idToken.isBlank() || datos.localId.isBlank()) {
            return Result.failure(DomainException.Desconocido(IllegalStateException("Firebase no devolvió usuario")))
        }

        val sesion = SesionFirebase(
            uid = datos.localId,
            email = datos.email.ifBlank { email },
            idToken = datos.idToken,
            refreshToken = datos.refreshToken,
            expiraEnMs = vencimiento(datos.expiresIn),
        )
        almacen.guardar(sesion)
        usuario.value = sesion.aUsuario()
        return Result.success(sesion.aUsuario())
    }

    /** Pide un idToken nuevo con el refresh token. Se llama con la exclusión ya tomada. */
    private suspend fun renovar(sesion: SesionFirebase): Result<String> {
        if (config.apiKey.isBlank()) return Result.failure(faltaConfiguracion())

        val respuesta = try {
            cliente.submitForm(
                url = URL_RENOVAR_TOKEN,
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", sesion.refreshToken)
                },
            ) {
                parameter("key", config.apiKey)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin conexión no se pierde la sesión: se reintenta la próxima vez.
            return Result.failure(DomainException.SinConexion)
        }

        if (!respuesta.status.isSuccess()) {
            val mensaje = respuesta.mensajeDeErrorFirebase()
            if (codigoDeErrorFirebase(mensaje) in CODIGOS_SESION_INVALIDA) {
                // El refresh token ya no sirve: se cierra la sesión y la app vuelve al login.
                cerrarSesion()
                return Result.failure(DomainException.SesionNoIniciada)
            }
            return Result.failure(mapearErrorAuthRest(mensaje, respuesta.status.value))
        }

        val datos = try {
            respuesta.body<RenovarRespuesta>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(DomainException.Desconocido(e))
        }

        if (datos.idToken.isBlank()) {
            return Result.failure(DomainException.Desconocido(IllegalStateException("Firebase no devolvió un token")))
        }

        val nueva = sesion.copy(
            idToken = datos.idToken,
            refreshToken = datos.refreshToken.ifBlank { sesion.refreshToken },
            expiraEnMs = vencimiento(datos.expiresIn),
        )
        almacen.guardar(nueva)
        return Result.success(nueva.idToken)
    }

    private fun vencimiento(expiresIn: String): Long {
        val segundos = ((expiresIn.toLongOrNull() ?: 3600L) - MARGEN_VENCIMIENTO_SEGUNDOS).coerceAtLeast(0L)
        return ahora() + segundos * 1000
    }

    private fun faltaConfiguracion() =
        DomainException.Desconocido(IllegalStateException("Falta FIREBASE_API_KEY en local.properties"))
}
