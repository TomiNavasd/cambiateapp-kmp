package com.example.cambiateappkmp.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

private val EXTENSIONES_DE_IMAGEN = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

/** Dice si un nombre de archivo es una imagen que sabemos subir y mostrar, según su extensión. */
internal fun esArchivoDeImagen(nombre: String): Boolean =
    nombre.substringAfterLast('.', "").lowercase() in EXTENSIONES_DE_IMAGEN

@Composable
actual fun rememberSelectorImagen(onImagenElegida: (String) -> Unit): SelectorImagen {
    val alElegir = rememberUpdatedState(onImagenElegida)

    return remember {
        object : SelectorImagen {
            override val camaraDisponible = false

            override fun abrirGaleria() {
                val dialogo = FileDialog(null as Frame?, "Elegir foto", FileDialog.LOAD)
                // Windows toma el filtro como un patrón en el nombre del archivo; los otros sistemas usan filenameFilter.
                dialogo.file = EXTENSIONES_DE_IMAGEN.joinToString(";") { "*.$it" }
                dialogo.setFilenameFilter { _, nombre -> esArchivoDeImagen(nombre) }
                dialogo.isVisible = true // se queda esperando hasta que el usuario elige o cancela

                val nombre = dialogo.file
                val carpeta = dialogo.directory
                // Si el usuario escribió a mano otro tipo de archivo, no lo dejamos pasar.
                if (nombre != null && carpeta != null && esArchivoDeImagen(nombre)) {
                    alElegir.value(File(carpeta, nombre).toURI().toString())
                }
            }

            override fun abrirCamara() = Unit
        }
    }
}
