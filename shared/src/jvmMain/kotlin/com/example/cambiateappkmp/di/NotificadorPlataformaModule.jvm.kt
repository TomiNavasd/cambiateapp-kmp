package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.domain.notification.Notificador
import org.koin.core.module.Module
import org.koin.dsl.module

/** En escritorio no hay recordatorios: el notificador no hace nada (la pantalla de Notificaciones no es accesible). */
private object NotificadorInerte : Notificador {
    override fun programarDiaria(hora: Int, minuto: Int) = Unit
    override fun cancelarDiaria() = Unit
    override fun mostrarAhora(titulo: String, mensaje: String) = Unit
}

actual val notificadorPlataformaModule: Module = module {
    single<Notificador> { NotificadorInerte }
}
