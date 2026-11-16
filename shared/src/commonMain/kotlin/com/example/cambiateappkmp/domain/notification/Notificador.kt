package com.example.cambiateappkmp.domain.notification

interface Notificador {
    /** Programa el aviso para que se repita todos los días a esa hora. Reemplaza la programación anterior. */
    fun programarDiaria(hora: Int, minuto: Int)

    fun cancelarDiaria()

    /** Muestra una notificación ahora. Si no hay permiso, no hace nada. */
    fun mostrarAhora(titulo: String, mensaje: String)
}
