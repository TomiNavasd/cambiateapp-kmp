package com.example.cambiateappkmp.presentation.notificaciones

/**
 * true si la plataforma tiene recordatorios del clima (Android). En escritorio no hay:
 * ahí no se muestra el acceso a Notificaciones ni se programa ningún recordatorio.
 */
expect val notificacionesDisponibles: Boolean
