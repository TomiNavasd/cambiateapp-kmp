package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.data.remote.PrendaDto
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda

internal fun PrendaDto.aDomain(id: String): Prenda? {
    val categoriaDominio = categoria.aCategoriaOrNull() ?: return null
    return Prenda(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        categoria = categoriaDominio,
        colores = colores,
        ocasiones = ocasiones.mapNotNull { it.aOcasionOrNull() },
        fotoUrl = fotoUrl,
    )
}

internal fun Prenda.aDto(userId: String): PrendaDto = PrendaDto(
    userId = userId,
    nombre = nombre,
    descripcion = descripcion,
    categoria = categoria.name.lowercase(),
    colores = colores,
    ocasiones = ocasiones.map { it.name.lowercase() },
    fotoUrl = fotoUrl,
)

private fun String.aCategoriaOrNull(): Categoria? =
    Categoria.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }

private fun String.aOcasionOrNull(): Ocasion? =
    Ocasion.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }