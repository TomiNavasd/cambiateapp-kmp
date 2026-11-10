package com.example.cambiateappkmp.di

import android.content.Context
import android.net.Uri
import com.example.cambiateappkmp.data.remote.LectorDeImagen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module

private class LectorDeImagenAndroid(private val contexto: Context) : LectorDeImagen {
    override suspend fun leerBytes(uri: String): ByteArray = withContext(Dispatchers.IO) {
        contexto.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
            ?: error("No se pudo abrir la imagen")
    }
}

actual val lectorImagenModule: Module = module {
    single<LectorDeImagen> { LectorDeImagenAndroid(get<Context>()) }
}
