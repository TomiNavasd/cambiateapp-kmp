package com.example.cambiateappkmp.presentation.permisos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberSolicitudNotificaciones(onResultado: (Boolean) -> Unit): SolicitudPermiso {
    val alResponder by rememberUpdatedState(onResultado)

    return remember {
        object : SolicitudPermiso {
            // En escritorio no hay notificaciones: la solicitud siempre dice que no.
            override val concedido: Boolean = false

            override fun pedir() = alResponder(false)
        }
    }
}
