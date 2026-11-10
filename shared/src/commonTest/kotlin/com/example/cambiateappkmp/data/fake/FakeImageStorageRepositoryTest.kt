package com.example.cambiateappkmp.data.fake

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FakeImageStorageRepositoryTest {

    @Test
    fun subirImagen_devuelve_exito_con_una_url_no_vacia() = runTest {
        val resultado = FakeImageStorageRepository().subirImagen("cualquier-uri")

        assertTrue(resultado.isSuccess)
        assertTrue(resultado.getOrNull().orEmpty().isNotBlank())
    }
}
