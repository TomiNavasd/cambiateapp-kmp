package com.example.cambiateappkmp.domain.repository

import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.PronosticoManiana

interface ClimaRepository {
    suspend fun obtenerClimaActual(lat: Double, lng: Double): Result<ClimaActual>
    suspend fun obtenerPronosticoManiana(lat: Double, lng: Double): Result<PronosticoManiana>
}
