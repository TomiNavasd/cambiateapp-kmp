package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.repository.AuthRepositoryImpl
import com.example.cambiateappkmp.domain.repository.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val authPlataformaModule: Module = module {
    single<AuthRepository> { AuthRepositoryImpl() }
}
