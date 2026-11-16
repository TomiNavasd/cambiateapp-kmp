package com.example.cambiateappkmp.presentation.permisos

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberSolicitudUbicacion(onResultado: (Boolean) -> Unit): SolicitudPermiso {
    val contexto = LocalContext.current
    val alResponder by rememberUpdatedState(onResultado)

    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { resultado ->
        // Alcanza con que acepte la ubicación aproximada o la precisa.
        alResponder(resultado.values.any { it })
    }

    return remember(lanzador, contexto) {
        object : SolicitudPermiso {
            override val concedido: Boolean
                get() = tienePermisoUbicacion(contexto)

            override fun pedir() {
                lanzador.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
    }
}
