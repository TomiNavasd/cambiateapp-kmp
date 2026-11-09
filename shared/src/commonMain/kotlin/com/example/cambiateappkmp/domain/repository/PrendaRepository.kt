package com.example.cambiateappkmp.domain.repository

import com.example.cambiateappkmp.domain.model.Prenda
import kotlinx.coroutines.flow.Flow

interface PrendaRepository {
    fun observarPrendas(): Flow<List<Prenda>>

    suspend fun obtenerPrenda(id: String): Prenda?

    suspend fun guardarPrenda(prenda: Prenda): Result<Unit>
}