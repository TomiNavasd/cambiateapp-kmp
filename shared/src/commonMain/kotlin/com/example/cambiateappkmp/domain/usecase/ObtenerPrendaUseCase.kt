package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.PrendaRepository

class ObtenerPrendaUseCase(
    private val repository: PrendaRepository,
) {
    suspend operator fun invoke(id: String): Prenda? = repository.obtenerPrenda(id)
}