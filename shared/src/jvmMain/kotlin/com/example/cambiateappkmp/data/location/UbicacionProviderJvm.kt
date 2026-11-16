package com.example.cambiateappkmp.data.location

import com.example.cambiateappkmp.domain.error.UbicacionException
import com.example.cambiateappkmp.domain.model.Ubicacion
import com.example.cambiateappkmp.domain.repository.UbicacionProvider

class UbicacionProviderJvm : UbicacionProvider {
    override suspend fun obtenerUbicacionActual(): Result<Ubicacion> =
        Result.failure(UbicacionException.NoDisponible("Ubicación no disponible en escritorio"))
}
