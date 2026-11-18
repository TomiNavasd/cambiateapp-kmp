package com.example.cambiateappkmp.presentation.components

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache

private const val CACHE_MAXIMA_BYTES = 256L * 1024 * 1024

/**
 * Configura Coil para escritorio: una caché de memoria más generosa (hasta 256 MB, sin pasar de un tercio de la memoria
 * de la JVM) para que las fotos de la grilla no se vuelvan a descargar y decodificar cada vez que se hace scroll.
 * Hay que llamarla una vez, antes de abrir la ventana. Si ya hay un cargador creado, no hace nada.
 */
fun configurarImagenesEscritorio() {
    SingletonImageLoader.setSafe { contexto ->
        val maximo = minOf(CACHE_MAXIMA_BYTES, Runtime.getRuntime().maxMemory() / 3)
        ImageLoader.Builder(contexto)
            .memoryCache { MemoryCache.Builder().maxSizeBytes(maximo).build() }
            .build()
    }
}
