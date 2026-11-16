package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakePreferenciasRepository
import com.example.cambiateappkmp.data.repository.PreferenciasRepositoryImpl
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository
import org.koin.dsl.module

// Versión real: guarda en el almacenamiento de cada plataforma.
val ajustesModule = module {
    includes(almacenClaveValorModule)
    single<PreferenciasRepository> { PreferenciasRepositoryImpl(get()) }
}

// Versión para pruebas: en memoria.
val ajustesFakeModule = module {
    single<PreferenciasRepository> { FakePreferenciasRepository() }
}
