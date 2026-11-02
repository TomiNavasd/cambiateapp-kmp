package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeAuthRepository
import com.example.cambiateappkmp.data.repository.AuthRepositoryImpl
import com.example.cambiateappkmp.domain.repository.AuthRepository
import com.example.cambiateappkmp.domain.usecase.LoginUseCase
import com.example.cambiateappkmp.domain.usecase.ObserveAuthStateUseCase
import com.example.cambiateappkmp.domain.usecase.RegisterUseCase
import com.example.cambiateappkmp.domain.usecase.SignOutUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl() }
    casosDeUsoDeAuth()
}

val authFakeModule = module {
    single<AuthRepository> { FakeAuthRepository() }
    casosDeUsoDeAuth()
}

private fun Module.casosDeUsoDeAuth() {
    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { SignOutUseCase(get()) }
    factory { ObserveAuthStateUseCase(get()) }
}
