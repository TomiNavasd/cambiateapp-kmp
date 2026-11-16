package com.example.cambiateappkmp.di

import android.content.Context
import com.example.cambiateappkmp.data.notification.NotificadorAndroid
import com.example.cambiateappkmp.domain.notification.Notificador
import org.koin.core.module.Module
import org.koin.dsl.module

actual val notificadorPlataformaModule: Module = module {
    single<Notificador> { NotificadorAndroid(get<Context>()) }
}
