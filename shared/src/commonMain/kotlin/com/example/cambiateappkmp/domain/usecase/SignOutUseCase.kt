package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.repository.AuthRepository

class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.cerrarSesion()
}
