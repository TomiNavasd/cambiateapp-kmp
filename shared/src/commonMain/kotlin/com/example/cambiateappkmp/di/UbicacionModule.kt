package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeUbicacionProvider
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import org.koin.dsl.module

// Versión real: el proveedor de cada plataforma.
val ubicacionModule = module {
    includes(ubicacionPlataformaModule)
}

val ubicacionFakeModule = module {
    single<UbicacionProvider> { FakeUbicacionProvider() }
}
