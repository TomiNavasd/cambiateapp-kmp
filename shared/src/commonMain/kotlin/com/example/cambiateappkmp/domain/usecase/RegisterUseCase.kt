package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Usuario
import com.example.cambiateappkmp.domain.repository.AuthRepository

class RegisterUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, pass: String): Result<Usuario> {
        val emailLimpio = email.trim()
        if (!esEmailValido(emailLimpio)) {
            return Result.failure(DomainException.EmailInvalido)
        }
        if (pass.length < LARGO_MINIMO_CONTRASENA) {
            return Result.failure(DomainException.ContrasenaDebil)
        }
        return authRepository.registrar(emailLimpio, pass)
    }

    private companion object {
        const val LARGO_MINIMO_CONTRASENA = 6
    }
}
