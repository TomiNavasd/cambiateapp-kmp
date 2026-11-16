package com.example.cambiateappkmp.di

import android.content.Context
import com.example.cambiateappkmp.data.location.UbicacionProviderAndroid
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import org.koin.core.module.Module
import org.koin.dsl.module

actual val ubicacionPlataformaModule: Module = module {
    single<UbicacionProvider> { UbicacionProviderAndroid(get<Context>()) }
}
