package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.domain.error.DomainException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlin.coroutines.cancellation.CancellationException

/** Firebase manda el código y, a veces, un detalle: "WEAK_PASSWORD : Password should be at least 6 characters". */
internal fun codigoDeErrorFirebase(mensaje: String?): String =
    mensaje.orEmpty().substringBefore(" : ").trim()

/**
 * Traduce un error de la API REST de Firebase Auth a los mismos errores de dominio que
 * ya usa Android (así las pantallas muestran los mismos mensajes).
 */
internal fun mapearErrorAuthRest(mensajeFirebase: String?, codigoHttp: Int): DomainException =
    when (val codigo = codigoDeErrorFirebase(mensajeFirebase)) {
        "EMAIL_NOT_FOUND",
        "INVALID_PASSWORD",
        "INVALID_LOGIN_CREDENTIALS",
        "USER_DISABLED",
        -> DomainException.CredencialesInvalidas

        "EMAIL_EXISTS" -> DomainException.EmailYaRegistrado
        "WEAK_PASSWORD" -> DomainException.ContrasenaDebil
        "INVALID_EMAIL" -> DomainException.EmailInvalido

        "TOO_MANY_ATTEMPTS_TRY_LATER" -> DomainException.Desconocido(
            IllegalStateException("Demasiados intentos: Firebase bloqueó el acceso por un rato"),
        )

        else -> DomainException.Desconocido(
            IllegalStateException("Firebase respondió $codigoHttp${if (codigo.isEmpty()) "" else ": $codigo"}"),
        )
    }

/** Lee el mensaje de error de una respuesta fallida. Devuelve null si el cuerpo no es el esperado. */
internal suspend fun HttpResponse.mensajeDeErrorFirebase(): String? =
    try {
        jsonFirebase.decodeFromString<ErrorFirebaseRespuesta>(bodyAsText()).error?.message
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
