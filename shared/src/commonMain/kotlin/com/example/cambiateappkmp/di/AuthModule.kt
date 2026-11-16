package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeAuthRepository
import com.example.cambiateappkmp.domain.repository.AuthRepository
import com.example.cambiateappkmp.domain.usecase.LoginUseCase
import com.example.cambiateappkmp.domain.usecase.ObserveAuthStateUseCase
import com.example.cambiateappkmp.domain.usecase.RegisterUseCase
import com.example.cambiateappkmp.domain.usecase.SignOutUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

// Versión real: el AuthRepository lo aporta cada plataforma (GitLive en Android, REST en escritorio).
val authModule = module {
    includes(authPlataformaModule)
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
