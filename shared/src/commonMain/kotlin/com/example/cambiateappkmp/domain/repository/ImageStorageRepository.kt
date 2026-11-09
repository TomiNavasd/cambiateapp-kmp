package com.example.cambiateappkmp.domain.repository

interface ImageStorageRepository {
    suspend fun subirImagen(uri: String): Result<String>
}