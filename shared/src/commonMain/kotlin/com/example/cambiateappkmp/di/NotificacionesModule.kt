package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.presentation.avisos.AvisosEnApp
import com.example.cambiateappkmp.presentation.avisos.AvisosEnAppImpl
import com.example.cambiateappkmp.presentation.notificaciones.NotificacionesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Los dos modulos usan el notificador de cada plataforma: la notificación de prueba se ve también en modo fake.
val notificacionesModule = module {
    includes(notificadorPlataformaModule)
    single<AvisosEnApp> { AvisosEnAppImpl() }
    viewModelOf(::NotificacionesViewModel)
}

val notificacionesFakeModule = module {
    includes(notificadorPlataformaModule)
    single<AvisosEnApp> { AvisosEnAppImpl() }
    viewModelOf(::NotificacionesViewModel)
}
