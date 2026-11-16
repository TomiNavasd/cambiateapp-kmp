package com.example.cambiateappkmp.presentation.avisos

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class AvisoEnApp(val titulo: String, val mensaje: String)

/**
 * Avisos que se muestran dentro de la app. Sirven de respaldo cuando el sistema no puede mostrar
 * una notificación (por ejemplo, en un escritorio sin bandeja del sistema).
 */
interface AvisosEnApp {
    val avisos: Flow<AvisoEnApp>

    fun mostrar(titulo: String, mensaje: String)
}

class AvisosEnAppImpl : AvisosEnApp {
    private val flujo = MutableSharedFlow<AvisoEnApp>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val avisos: Flow<AvisoEnApp> = flujo.asSharedFlow()

    override fun mostrar(titulo: String, mensaje: String) {
        flujo.tryEmit(AvisoEnApp(titulo, mensaje))
    }
}
