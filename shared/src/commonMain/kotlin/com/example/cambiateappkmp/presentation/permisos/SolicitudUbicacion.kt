package com.example.cambiateappkmp.presentation.permisos

import androidx.compose.runtime.Composable

/**
 * Crea la solicitud del permiso de ubicación. Alcanza con que el usuario acepte la ubicación
 * aproximada o la precisa. [onResultado] recibe true si quedó concedido.
 */
@Composable
expect fun rememberSolicitudUbicacion(onResultado: (Boolean) -> Unit): SolicitudPermiso
