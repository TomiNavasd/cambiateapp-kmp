package com.example.cambiateappkmp.di

import android.content.Context
import com.example.cambiateappkmp.data.preferences.AlmacenClaveValor
import com.example.cambiateappkmp.data.preferences.AlmacenClaveValorAndroid
import org.koin.core.module.Module
import org.koin.dsl.module

actual val almacenClaveValorModule: Module = module {
    single<AlmacenClaveValor> {
        AlmacenClaveValorAndroid(get<Context>().getSharedPreferences("cambiateapp_prefs", Context.MODE_PRIVATE))
    }
}
