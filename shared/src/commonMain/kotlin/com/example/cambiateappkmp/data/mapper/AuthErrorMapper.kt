package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.domain.error.DomainException
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.code

internal enum class TipoErrorAuth {
    CREDENCIALES_INVALIDAS,
    USUARIO_INVALIDO,
    COLISION_USUARIO,
    CONTRASENA_DEBIL,
    RED,
    OTRO
}

internal const val CODIGO_EMAIL_INVALIDO = "ERROR_INVALID_EMAIL"

internal fun Throwable.aDomainException(): DomainException {
    if (this is DomainException) return this
    val codigo = (this as? FirebaseAuthException)?.code
    return mapearErrorDeAuth(clasificarErrorDeAuth(this), codigo, this)
}

internal fun clasificarErrorDeAuth(error: Throwable): TipoErrorAuth = when (error) {
    // FirebaseAuthWeakPasswordException hereda de InvalidCredentials: va primero
    is FirebaseAuthWeakPasswordException -> TipoErrorAuth.CONTRASENA_DEBIL
    is FirebaseAuthUserCollisionException -> TipoErrorAuth.COLISION_USUARIO
    is FirebaseAuthInvalidCredentialsException -> TipoErrorAuth.CREDENCIALES_INVALIDAS
    is FirebaseAuthInvalidUserException -> TipoErrorAuth.USUARIO_INVALIDO
    is FirebaseNetworkException -> TipoErrorAuth.RED
    else -> TipoErrorAuth.OTRO
}

internal fun mapearErrorDeAuth(tipo: TipoErrorAuth, codigo: String?, causa: Throwable): DomainException {
    if (codigo == CODIGO_EMAIL_INVALIDO) return DomainException.EmailInvalido
    return when (tipo) {
        TipoErrorAuth.CONTRASENA_DEBIL -> DomainException.ContrasenaDebil
        TipoErrorAuth.COLISION_USUARIO -> DomainException.EmailYaRegistrado
        TipoErrorAuth.CREDENCIALES_INVALIDAS,
        TipoErrorAuth.USUARIO_INVALIDO -> DomainException.CredencialesInvalidas
        TipoErrorAuth.RED -> DomainException.SinConexion
        TipoErrorAuth.OTRO -> DomainException.Desconocido(causa)
    }
}
