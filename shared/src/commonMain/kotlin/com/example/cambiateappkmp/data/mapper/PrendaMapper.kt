package com.example.cambiateappkmp.data.mapper

import com.example.cambiateappkmp.data.remote.PrendaDto
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda

/**
 * Los únicos campos que escribe la app al editar una prenda: los del formulario más las etiquetas, que se conservan
 * tal como se cargaron. La app nativa guarda más campos (temporada, vecesUsada, ultimaVezUsada, lugarCompra, lat, lng)
 * y el userId no se cambia: al editar nunca se escriben, así que una prenda de la nativa no pierde nada.
 * Es lo que la nativa llama CAMPOS_EDITABLES (mergeFields) y lo que acá es el updateMask de REST.
 */
internal val CAMPOS_EDITABLES_PRENDA = listOf(
    "nombre", "descripcion", "categoria", "colores", "ocasiones", "fotoUrl", "etiquetas",
)

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
        etiquetas = etiquetas,
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
    etiquetas = etiquetas,
)

private fun String.aCategoriaOrNull(): Categoria? =
    Categoria.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }

private fun String.aOcasionOrNull(): Ocasion? =
    Ocasion.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }