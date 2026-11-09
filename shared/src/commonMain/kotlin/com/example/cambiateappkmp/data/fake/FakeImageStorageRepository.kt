package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.domain.repository.ImageStorageRepository
import kotlinx.coroutines.delay

class FakeImageStorageRepository : ImageStorageRepository {
    override suspend fun subirImagen(uri: String): Result<String> {
        delay(500)
        return Result.success("https://picsum.photos/seed/cambiateapp/400/400")
    }
}