package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.model.Usuario
import com.example.cambiateappkmp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Usuario?> = authRepository.usuarioActual
}
