package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.domain.error.DomainException
import dev.gitlive.firebase.FirebaseNetworkException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class AuthErrorMapperAndroidTest {

    @Test
    fun contrasenaDebilRealSeMapeaAContrasenaDebil() {
        val error = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "muy corta", "6 caracteres").aDomainException()

        assertSame(DomainException.ContrasenaDebil, error)
    }

    @Test
    fun colisionRealSeMapeaAEmailYaRegistrado() {
        val error = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "ya existe").aDomainException()

        assertSame(DomainException.EmailYaRegistrado, error)
    }

    @Test
    fun credencialInvalidaRealSeMapeaACredencialesInvalidas() {
        val error = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "mal").aDomainException()

        assertSame(DomainException.CredencialesInvalidas, error)
    }

    @Test
    fun emailMalformadoRealSeMapeaAEmailInvalido() {
        val error = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "mal formado").aDomainException()

        assertSame(DomainException.EmailInvalido, error)
    }

    @Test
    fun usuarioInexistenteRealSeMapeaACredencialesInvalidas() {
        val error = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "no existe").aDomainException()

        assertSame(DomainException.CredencialesInvalidas, error)
    }

    @Test
    fun errorDeRedRealSeMapeaASinConexion() {
        val error = FirebaseNetworkException("sin red").aDomainException()

        assertSame(DomainException.SinConexion, error)
    }

    @Test
    fun excepcionNoReconocidaSeMapeaADesconocido() {
        val causa = IllegalStateException("otra cosa")

        val error = causa.aDomainException()

        assertIs<DomainException.Desconocido>(error)
        assertSame(causa, error.cause)
    }

    @Test
    fun unaDomainExceptionPasaSinCambios() {
        assertSame(DomainException.SesionNoIniciada, DomainException.SesionNoIniciada.aDomainException())
    }
}
