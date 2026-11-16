package com.example.cambiateappkmp.presentation.notificaciones

data class NotificacionesUiState(
    val activadas: Boolean = false,
    val hora: Int = 20,
    val minuto: Int = 0,
    val aviso: String? = null,
)

/** Devuelve la hora con dos dígitos, por ejemplo 08:05. */
internal fun formatearHora(hora: Int, minuto: Int): String =
    "${hora.toString().padStart(2, '0')}:${minuto.toString().padStart(2, '0')}"
