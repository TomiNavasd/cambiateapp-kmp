package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakePrendaRepository
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import com.example.cambiateappkmp.domain.usecase.ObservePrendasUseCase
import com.example.cambiateappkmp.domain.usecase.ObtenerPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.SavePrendaUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

// Versión real: el PrendaRepository lo aporta cada plataforma (GitLive en Android, REST en escritorio).
val prendasModule = module {
    includes(prendasPlataformaModule)
    casosDeUsoDePrendas()
}

val prendasFakeModule = module {
    single<PrendaRepository> { FakePrendaRepository() }
    casosDeUsoDePrendas()
}

private fun Module.casosDeUsoDePrendas() {
    factory { ObservePrendasUseCase(get()) }
    factory { ObtenerPrendaUseCase(get()) }
    factory { SavePrendaUseCase(get()) }
}