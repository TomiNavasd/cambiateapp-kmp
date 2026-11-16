package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.preferences.AlmacenClaveValor
import com.example.cambiateappkmp.data.preferences.AlmacenClaveValorPrefs
import org.koin.core.module.Module
import org.koin.dsl.module

actual val almacenClaveValorModule: Module = module {
    single<AlmacenClaveValor> { AlmacenClaveValorPrefs() }
}
