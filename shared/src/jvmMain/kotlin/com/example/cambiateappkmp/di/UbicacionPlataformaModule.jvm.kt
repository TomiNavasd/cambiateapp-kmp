package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.location.UbicacionProviderJvm
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual val ubicacionPlataformaModule: Module = module {
    single<UbicacionProvider> { UbicacionProviderJvm() }
}
