package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.remote.LectorDeImagen
import java.io.File
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.module.Module
import org.koin.dsl.module

private class LectorDeImagenJvm : LectorDeImagen {
    override suspend fun leerBytes(uri: String): ByteArray = withContext(Dispatchers.IO) {
        File(URI(uri)).readBytes()
    }
}

actual val lectorImagenModule: Module = module {
    single<LectorDeImagen> { LectorDeImagenJvm() }
}
