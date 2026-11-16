package com.example.cambiateappkmp.presentation.permisos

import androidx.compose.runtime.Composable

/** Crea la solicitud del permiso de notificaciones. [onResultado] recibe true si quedó concedido. */
@Composable
expect fun rememberSolicitudNotificaciones(onResultado: (Boolean) -> Unit): SolicitudPermiso
