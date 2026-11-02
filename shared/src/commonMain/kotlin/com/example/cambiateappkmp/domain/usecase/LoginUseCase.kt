package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.error.CampoInvalido
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Usuario
import com.example.cambiateappkmp.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, pass: String): Result<Usuario> {
        val emailLimpio = email.trim()
        if (!esEmailValido(emailLimpio)) {
            return Result.failure(DomainException.EmailInvalido)
        }
        if (pass.isEmpty()) {
            return Result.failure(DomainException.Validacion(CampoInvalido.CONTRASENA))
        }
        return authRepository.login(emailLimpio, pass)
    }
}
