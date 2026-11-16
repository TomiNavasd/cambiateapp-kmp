package com.example.cambiateappkmp.presentation.permisos

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSolicitudNotificaciones(onResultado: (Boolean) -> Unit): SolicitudPermiso {
    val contexto = LocalContext.current
    val alResponder by rememberUpdatedState(onResultado)

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        alResponder(concedido)
    }

    return remember(lanzador, contexto) {
        object : SolicitudPermiso {
            override val concedido: Boolean
                get() = tienePermisoNotificaciones(contexto)

            override fun pedir() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Antes de Android 13 no hay permiso que pedir.
                    alResponder(true)
                }
            }
        }
    }
}
