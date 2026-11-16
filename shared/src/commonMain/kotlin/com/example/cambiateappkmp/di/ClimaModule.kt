package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeClimaRepository
import com.example.cambiateappkmp.data.repository.ClimaRepositoryImpl
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Cliente propio del clima: no se comparte con el de las imágenes.
private fun crearClienteClima() = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
    }
}

// Versión real: Open-Meteo (sin clave).
val climaModule = module {
    single(named("clima")) { crearClienteClima() }
    single<ClimaRepository> { ClimaRepositoryImpl(get(named("clima"))) }
}

val climaFakeModule = module {
    single<ClimaRepository> { FakeClimaRepository() }
}
