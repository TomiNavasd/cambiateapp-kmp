package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.rest.AlmacenSesionEnMemoria
import com.example.cambiateappkmp.data.rest.AuthRepositoryRest
import com.example.cambiateappkmp.data.rest.jsonFirebase
import com.example.cambiateappkmp.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Cliente propio para hablar con Firebase por REST (autenticación y, más adelante, Firestore).
internal fun crearClienteFirebaseRest() = HttpClient {
    install(ContentNegotiation) { json(jsonFirebase) }
    install(HttpTimeout) { requestTimeoutMillis = 30_000 }
}

actual val authPlataformaModule: Module = module {
    single(named("firebaseRest")) { crearClienteFirebaseRest() }
    single<AuthRepository> {
        AuthRepositoryRest(
            cliente = get(named("firebaseRest")),
            config = get(),
            almacen = AlmacenSesionEnMemoria(),
        )
    }
}
