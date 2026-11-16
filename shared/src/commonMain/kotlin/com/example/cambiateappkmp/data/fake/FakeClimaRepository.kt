package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.PronosticoManiana
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import kotlinx.coroutines.delay

/** Datos de ejemplo. Mañana llueve con 70 % de probabilidad, para poder probar la notificación. */
class FakeClimaRepository : ClimaRepository {

    override suspend fun obtenerClimaActual(lat: Double, lng: Double): Result<ClimaActual> {
        delay(300)
        return Result.success(
            ClimaActual(temperaturaC = 22.0, descripcion = "Parcialmente nublado", llueve = false),
        )
    }

    override suspend fun obtenerPronosticoManiana(lat: Double, lng: Double): Result<PronosticoManiana> {
        delay(300)
        return Result.success(
            PronosticoManiana(maximaC = 24.0, minimaC = 16.0, probabilidadLluvia = 70, descripcion = "Lluvia"),
        )
    }
}
