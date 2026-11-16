package com.example.cambiateappkmp.presentation.permisos

/** Un permiso del sistema que la app puede pedirle al usuario. */
interface SolicitudPermiso {
    /** true si el permiso ya está concedido. */
    val concedido: Boolean

    /** Muestra el diálogo del sistema. El resultado llega por el callback de quien creó la solicitud. */
    fun pedir()
}
