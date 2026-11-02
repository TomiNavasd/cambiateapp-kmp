package com.example.cambiateappkmp.domain.repository

import com.example.cambiateappkmp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val usuarioActual: Flow<Usuario?>
    suspend fun login(email: String, pass: String): Result<Usuario>
    suspend fun registrar(email: String, pass: String): Result<Usuario>
    suspend fun cerrarSesion()
}
