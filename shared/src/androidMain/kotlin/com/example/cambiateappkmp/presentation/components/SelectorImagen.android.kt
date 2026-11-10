package com.example.cambiateappkmp.presentation.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberSelectorImagen(onImagenElegida: (String) -> Unit): SelectorImagen {
    val contexto = LocalContext.current
    val alElegir by rememberUpdatedState(onImagenElegida)

    // Archivo donde la cámara va a dejar la foto; lo recordamos hasta que vuelva el resultado.
    var uriCamara by remember { mutableStateOf<Uri?>(null) }

    val galeria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) alElegir(uri.toString())
    }

    val camara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { sacada ->
        val uri = uriCamara
        if (sacada && uri != null) alElegir(uri.toString())
    }

    return remember(galeria, camara, contexto) {
        object : SelectorImagen {
            override val camaraDisponible = true

            override fun abrirGaleria() {
                galeria.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }

            override fun abrirCamara() {
                val uri = crearUriTemporal(contexto)
                uriCamara = uri
                camara.launch(uri)
            }
        }
    }
}

/** Crea un archivo vacío en la caché de la app y devuelve una uri segura (FileProvider) para la cámara. */
private fun crearUriTemporal(contexto: Context): Uri {
    val carpeta = File(contexto.cacheDir, "fotos").apply { mkdirs() }
    val archivo = File.createTempFile("prenda_", ".jpg", carpeta)
    return FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
}