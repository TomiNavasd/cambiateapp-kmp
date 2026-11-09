package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.error.CampoInvalido
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.PrendaRepository

class SavePrendaUseCase(
    private val repository: PrendaRepository,
) {
    suspend operator fun invoke(prenda: Prenda): Result<Unit> {
        val nombre = prenda.nombre.trim()
        if (nombre.isEmpty()) {
            return Result.failure(DomainException.Validacion(CampoInvalido.NOMBRE))
        }
        if (prenda.fotoUrl.isBlank()) {
            return Result.failure(DomainException.Validacion(CampoInvalido.FOTO))
        }
        return repository.guardarPrenda(prenda.copy(nombre = nombre))
    }
}