package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.rest.PrendaRepositoryRest
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val prendasPlataformaModule: Module = module {
    single<PrendaRepository> {
        PrendaRepositoryRest(
            cliente = get(named("firebaseRest")),
            config = get(),
            tokens = get(),
            auth = get(),
        )
    }
}
