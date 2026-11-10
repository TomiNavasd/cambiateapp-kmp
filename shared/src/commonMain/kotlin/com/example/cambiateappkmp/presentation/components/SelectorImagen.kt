package com.example.cambiateappkmp.presentation.components

import androidx.compose.runtime.Composable

/** Abre la galería o la cámara de la plataforma. Cada plataforma lo implementa a su manera. */
interface SelectorImagen {
    /** false si la plataforma no tiene cámara que podamos usar (por ejemplo, escritorio). */
    val camaraDisponible: Boolean

    fun abrirGaleria()

    fun abrirCamara()
}

/**
 * Crea un [SelectorImagen]. Cuando el usuario elige o saca una foto,
 * llama a [onImagenElegida] con la dirección (uri) de la imagen.
 */
@Composable
expect fun rememberSelectorImagen(onImagenElegida: (String) -> Unit): SelectorImagen