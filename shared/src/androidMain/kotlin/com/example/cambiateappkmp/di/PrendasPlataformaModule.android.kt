package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.repository.PrendaRepositoryImpl
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val prendasPlataformaModule: Module = module {
    single<PrendaRepository> { PrendaRepositoryImpl() }
}
