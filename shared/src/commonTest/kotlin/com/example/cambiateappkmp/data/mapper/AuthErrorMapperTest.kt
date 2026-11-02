package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.domain.error.DomainException
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class AuthErrorMapperTest {

    private val causa = RuntimeException("falla de prueba")

    @Test
    fun contrasenaDebilSeMapeaAContrasenaDebil() {
        val error = mapearErrorDeAuth(TipoErrorAuth.CONTRASENA_DEBIL, "ERROR_WEAK_PASSWORD", causa)

        assertSame(DomainException.ContrasenaDebil, error)
    }

    @Test
    fun colisionDeUsuarioSeMapeaAEmailYaRegistrado() {
        val error = mapearErrorDeAuth(TipoErrorAuth.COLISION_USUARIO, "ERROR_EMAIL_ALREADY_IN_USE", causa)

        assertSame(DomainException.EmailYaRegistrado, error)
    }

    @Test
    fun credencialInvalidaSeMapeaACredencialesInvalidas() {
        val error = mapearErrorDeAuth(TipoErrorAuth.CREDENCIALES_INVALIDAS, "ERROR_INVALID_CREDENTIAL", causa)

        assertSame(DomainException.CredencialesInvalidas, error)
    }

    @Test
    fun contrasenaIncorrectaSeMapeaACredencialesInvalidas() {
        val error = mapearErrorDeAuth(TipoErrorAuth.CREDENCIALES_INVALIDAS, "ERROR_WRONG_PASSWORD", causa)

        assertSame(DomainException.CredencialesInvalidas, error)
    }

    @Test
    fun emailMalformadoSeMapeaAEmailInvalido() {
        val error = mapearErrorDeAuth(TipoErrorAuth.CREDENCIALES_INVALIDAS, "ERROR_INVALID_EMAIL", causa)

        assertSame(DomainException.EmailInvalido, error)
    }

    @Test
    fun codigoDeEmailInvalidoGanaAunqueElTipoSeaOtro() {
        val error = mapearErrorDeAuth(TipoErrorAuth.OTRO, "ERROR_INVALID_EMAIL", causa)

        assertSame(DomainException.EmailInvalido, error)
    }

    @Test
    fun usuarioInexistenteSeMapeaACredencialesInvalidas() {
        val error = mapearErrorDeAuth(TipoErrorAuth.USUARIO_INVALIDO, "ERROR_USER_NOT_FOUND", causa)

        assertSame(DomainException.CredencialesInvalidas, error)
    }

    @Test
    fun errorDeRedSeMapeaASinConexion() {
        val error = mapearErrorDeAuth(TipoErrorAuth.RED, null, causa)

        assertSame(DomainException.SinConexion, error)
    }

    @Test
    fun cualquierOtroErrorSeMapeaADesconocidoConsevandoLaCausa() {
        val error = mapearErrorDeAuth(TipoErrorAuth.OTRO, null, causa)

        assertIs<DomainException.Desconocido>(error)
        assertSame(causa, error.cause)
    }
}
