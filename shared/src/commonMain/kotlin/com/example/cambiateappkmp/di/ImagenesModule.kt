package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeImageStorageRepository
import com.example.cambiateappkmp.domain.repository.ImageStorageRepository
import org.koin.dsl.module

// Versión real. TODO (T3): reemplazar por la implementación de Cloudinary.
// Mientras tanto usa el fake para que la app no falle al abrir el formulario.
val imagenesModule = module {
    single<ImageStorageRepository> { FakeImageStorageRepository() }
}

// Versión para pruebas y escritorio: siempre usa el fake.
val imagenesFakeModule = module {
    single<ImageStorageRepository> { FakeImageStorageRepository() }
}