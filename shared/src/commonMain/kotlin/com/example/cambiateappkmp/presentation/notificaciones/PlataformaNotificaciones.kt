package com.example.cambiateappkmp.presentation.notificaciones

/**
 * true si los recordatorios solo salen mientras la app está abierta (escritorio).
 * En Android los programa el sistema y salen aunque la app esté cerrada.
 */
expect val recordatoriosSoloConLaAppAbierta: Boolean
