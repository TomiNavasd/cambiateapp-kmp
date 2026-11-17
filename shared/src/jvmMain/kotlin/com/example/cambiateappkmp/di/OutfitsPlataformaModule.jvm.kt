package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.rest.OutfitsRepositoryRest
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val outfitsPlataformaModule: Module = module {
    single<OutfitsRepository> {
        OutfitsRepositoryRest(
            cliente = get(named("firebaseRest")),
            config = get(),
            tokens = get(),
            auth = get(),
        )
    }
}
