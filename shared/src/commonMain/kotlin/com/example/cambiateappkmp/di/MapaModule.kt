package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeLocalesRepository
import com.example.cambiateappkmp.data.repository.LocalesRepositoryImpl
import com.example.cambiateappkmp.domain.repository.LocalesRepository
import com.example.cambiateappkmp.presentation.mapa.MapaViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Cliente propio del mapa. Overpass pide un User-Agent que identifique a la app.
private fun crearClienteMapa() = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
    }
    install(UserAgent) {
        agent = "CambiateApp/1.0 (trabajo practico UNAJ; app Android)"
    }
}

// Versión real: locales de ropa desde OpenStreetMap (Overpass, sin clave).
val mapaModule = module {
    single(named("mapa")) { crearClienteMapa() }
    single<LocalesRepository> { LocalesRepositoryImpl(get(named("mapa"))) }
    viewModelOf(::MapaViewModel)
}

val mapaFakeModule = module {
    single<LocalesRepository> { FakeLocalesRepository() }
    viewModelOf(::MapaViewModel)
}
