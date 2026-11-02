package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Usuario
import com.example.cambiateappkmp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Repositorio de auth en memoria, sin Firebase. Arranca con una cuenta demo y sin sesión. */
class FakeAuthRepository : AuthRepository {
    private class Cuenta(val usuario: Usuario, val pass: String)

    private val cuentas = mutableMapOf<String, Cuenta>()
    private val sesion = MutableStateFlow<Usuario?>(null)
    private var siguienteId = 1

    override val usuarioActual: Flow<Usuario?> = sesion

    init {
        crearCuenta(EMAIL_DEMO, PASS_DEMO)
    }

    override suspend fun login(email: String, pass: String): Result<Usuario> {
        val cuenta = cuentas[email.lowercase()]
        if (cuenta == null || cuenta.pass != pass) {
            return Result.failure(DomainException.CredencialesInvalidas)
        }
        sesion.value = cuenta.usuario
        return Result.success(cuenta.usuario)
    }

    override suspend fun registrar(email: String, pass: String): Result<Usuario> {
        if (email.lowercase() in cuentas) {
            return Result.failure(DomainException.EmailYaRegistrado)
        }
        val usuario = crearCuenta(email, pass)
        sesion.value = usuario
        return Result.success(usuario)
    }

    override suspend fun cerrarSesion() {
        sesion.value = null
    }

    private fun crearCuenta(email: String, pass: String): Usuario {
        val usuario = Usuario(id = "fake-${siguienteId++}", email = email)
        cuentas[email.lowercase()] = Cuenta(usuario, pass)
        return usuario
    }

    companion object {
        const val EMAIL_DEMO = "demo@cambiateapp.com"
        const val PASS_DEMO = "123456"
    }
}
