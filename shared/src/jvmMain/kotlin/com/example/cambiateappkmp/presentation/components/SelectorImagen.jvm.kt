package com.example.cambiateappkmp.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberSelectorImagen(onImagenElegida: (String) -> Unit): SelectorImagen {
    val alElegir = rememberUpdatedState(onImagenElegida)

    return remember {
        object : SelectorImagen {
            override val camaraDisponible = false

            override fun abrirGaleria() {
                val dialogo = FileDialog(null as Frame?, "Elegir foto", FileDialog.LOAD)
                dialogo.isVisible = true // se queda esperando hasta que el usuario elige o cancela
                val nombre = dialogo.file
                if (nombre != null) {
                    alElegir.value(File(dialogo.directory, nombre).toURI().toString())
                }
            }

            override fun abrirCamara() = Unit
        }
    }
}