package com.example.cambiateappkmp.presentation.permisos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberSolicitudUbicacion(onResultado: (Boolean) -> Unit): SolicitudPermiso {
    val alResponder by rememberUpdatedState(onResultado)

    return remember {
        object : SolicitudPermiso {
            // En escritorio no hay permiso que pedir: la ubicación aproximada se calcula por IP.
            override val concedido: Boolean = true

            override fun pedir() = alResponder(true)
        }
    }
}
