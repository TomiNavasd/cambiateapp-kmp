package com.example.cambiateappkmp.data.notification

import com.example.cambiateappkmp.domain.notification.Notificador

/** En escritorio no hay notificaciones programadas: no hace nada. */
class NotificadorJvm : Notificador {
    override fun programarDiaria(hora: Int, minuto: Int) = Unit

    override fun cancelarDiaria() = Unit

    override fun mostrarAhora(titulo: String, mensaje: String) = Unit
}
