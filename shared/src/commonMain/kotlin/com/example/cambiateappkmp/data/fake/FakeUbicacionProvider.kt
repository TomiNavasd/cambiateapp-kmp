package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.domain.model.Ubicacion
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import kotlinx.coroutines.delay

/** Siempre devuelve Buenos Aires (el Obelisco). */
class FakeUbicacionProvider : UbicacionProvider {
    override suspend fun obtenerUbicacionActual(): Result<Ubicacion> {
        delay(200)
        return Result.success(Ubicacion(lat = -34.6037, lng = -58.3816))
    }
}
