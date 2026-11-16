package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.rest.AlmacenSesionPrefs
import com.example.cambiateappkmp.data.rest.AuthRepositoryRest
import com.example.cambiateappkmp.data.rest.TokenProvider
import com.example.cambiateappkmp.data.rest.jsonFirebase
import com.example.cambiateappkmp.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Cliente propio para hablar con Firebase por REST (autenticación y Firestore).
internal fun crearClienteFirebaseRest() = HttpClient {
    install(ContentNegotiation) { json(jsonFirebase) }
    install(HttpTimeout) { requestTimeoutMillis = 30_000 }
}

actual val authPlataformaModule: Module = module {
    single(named("firebaseRest")) { crearClienteFirebaseRest() }
    single {
        AuthRepositoryRest(
            cliente = get(named("firebaseRest")),
            config = get(),
            almacen = AlmacenSesionPrefs(),
        )
    }
    // El mismo objeto atiende como repositorio de auth y como proveedor del token para Firestore.
    single<AuthRepository> { get<AuthRepositoryRest>() }
    single<TokenProvider> { get<AuthRepositoryRest>() }
}
