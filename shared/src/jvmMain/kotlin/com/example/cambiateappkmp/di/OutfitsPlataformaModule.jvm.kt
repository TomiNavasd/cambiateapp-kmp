package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.rest.OutfitsRepositoryJvm
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val outfitsPlataformaModule: Module = module {
    single<OutfitsRepository> { OutfitsRepositoryJvm() }
}
