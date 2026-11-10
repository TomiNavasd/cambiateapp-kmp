package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeImageStorageRepository
import com.example.cambiateappkmp.data.repository.CloudinaryImageStorageRepository
import com.example.cambiateappkmp.domain.repository.ImageStorageRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

private fun crearClienteHttp() = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
    }
}

// Versión real: sube las fotos a Cloudinary.
val imagenesModule = module {
    includes(lectorImagenModule)
    single { crearClienteHttp() }
    single<ImageStorageRepository> { CloudinaryImageStorageRepository(get(), get()) }
}

// Versión para pruebas y escritorio: siempre usa el fake.
val imagenesFakeModule = module {
    single<ImageStorageRepository> { FakeImageStorageRepository() }
}
