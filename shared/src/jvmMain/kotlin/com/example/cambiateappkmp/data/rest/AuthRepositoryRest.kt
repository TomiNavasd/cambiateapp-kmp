package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.config.ConfigFirebaseEscritorio
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Usuario
import com.example.cambiateappkmp.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val URL_INICIAR_SESION = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"
private const val URL_REGISTRAR = "https://identitytoolkit.googleapis.com/v1/accounts:signUp"

/** Margen para renovar el token un poco antes de que venza. */
private const val MARGEN_VENCIMIENTO_SEGUNDOS = 60L

/** Autenticación de escritorio: habla con la API REST de Firebase Auth (GitLive no funciona bien en JVM). */
internal class AuthRepositoryRest(
    private val cliente: HttpClient,
    private val config: ConfigFirebaseEscritorio,
    private val almacen: AlmacenSesion,
    private val ahora: () -> Long = { System.currentTimeMillis() },
) : AuthRepository {

    private val usuario = MutableStateFlow(almacen.leer()?.aUsuario())

    override val usuarioActual: Flow<Usuario?> = usuario.asStateFlow()

    override suspend fun login(email: String, pass: String): Result<Usuario> =
        autenticar(URL_INICIAR_SESION, email, pass)

    override suspend fun registrar(email: String, pass: String): Result<Usuario> =
        autenticar(URL_REGISTRAR, email, pass)

    override suspend fun cerrarSesion() {
        almacen.borrar()
        usuario.value = null
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
            expiraEnMs = ahora() + segundosDeVida(datos.expiresIn) * 1000,
        )
        almacen.guardar(sesion)
        usuario.value = sesion.aUsuario()
        return Result.success(sesion.aUsuario())
    }

    private fun segundosDeVida(expiresIn: String): Long =
        ((expiresIn.toLongOrNull() ?: 3600L) - MARGEN_VENCIMIENTO_SEGUNDOS).coerceAtLeast(0L)

    private fun faltaConfiguracion() =
        DomainException.Desconocido(IllegalStateException("Falta FIREBASE_API_KEY en local.properties"))
}
