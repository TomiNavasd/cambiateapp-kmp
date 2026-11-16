package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.location.UbicacionProviderJvm
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Cliente propio para consultar la ubicación aproximada por IP.
internal fun crearClienteUbicacion() = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
    }
    install(UserAgent) {
        agent = "CambiateApp/1.0 (trabajo practico UNAJ; app de escritorio)"
    }
}

actual val ubicacionPlataformaModule: Module = module {
    single(named("ubicacionIp")) { crearClienteUbicacion() }
    single<UbicacionProvider> {
        UbicacionProviderJvm(
            cliente = get(named("ubicacionIp")),
            preferencias = get(),
        )
    }
}
