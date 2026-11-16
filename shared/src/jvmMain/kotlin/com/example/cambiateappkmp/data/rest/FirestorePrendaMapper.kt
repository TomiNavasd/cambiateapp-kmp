package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.remote.PrendaDto

/**
 * Los campos que la app puede editar. Coincide con lo que guarda Android (sin userId, que no se puede cambiar).
 * La app nativa guarda más campos (etiquetas, temporada, etc.): al editar solo se tocan estos, así no se pisan.
 */
internal val CAMPOS_EDITABLES_PRENDA = listOf("nombre", "descripcion", "categoria", "colores", "ocasiones", "fotoUrl")

/** Los campos de una prenda, con los mismos nombres y tipos que usa Android en la colección prendas. */
internal fun PrendaDto.aCamposFirestore(): Map<String, ValorFirestore> = mapOf(
    "userId" to ValorFirestore.Texto(userId),
    "nombre" to ValorFirestore.Texto(nombre),
    "descripcion" to ValorFirestore.Texto(descripcion),
    "categoria" to ValorFirestore.Texto(categoria),
    "colores" to ValorFirestore.Lista(colores.map(ValorFirestore::Texto)),
    "ocasiones" to ValorFirestore.Lista(ocasiones.map(ValorFirestore::Texto)),
    "fotoUrl" to ValorFirestore.Texto(fotoUrl),
)

/** Lo mismo que [aCamposFirestore] pero solo con los campos editables (para el PATCH con updateMask). */
internal fun PrendaDto.aCamposEditablesFirestore(): Map<String, ValorFirestore> =
    aCamposFirestore().filterKeys { it in CAMPOS_EDITABLES_PRENDA }

/** Lee una prenda de un documento. Lo que falte queda con el valor por defecto, como hace GitLive en Android. */
internal fun DocumentoFirestore.aPrendaDto(): PrendaDto = PrendaDto(
    userId = texto("userId").orEmpty(),
    nombre = texto("nombre").orEmpty(),
    descripcion = texto("descripcion").orEmpty(),
    categoria = texto("categoria").orEmpty(),
    colores = listaDeTextos("colores"),
    ocasiones = listaDeTextos("ocasiones"),
    fotoUrl = texto("fotoUrl").orEmpty(),
)
