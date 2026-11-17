package com.example.cambiateappkmp.data.rest

import com.example.cambiateappkmp.data.mapper.CAMPOS_EDITABLES_PRENDA
import com.example.cambiateappkmp.data.remote.PrendaDto

// CAMPOS_EDITABLES_PRENDA (compartido con Android) es la lista de campos que se tocan al editar, sin userId.
// La app nativa guarda más campos (temporada, vecesUsada, etc.): al editar solo se tocan estos, así no se pisan.

/** Los campos de una prenda, con los mismos nombres y tipos que usa Android en la colección prendas. */
internal fun PrendaDto.aCamposFirestore(): Map<String, ValorFirestore> = mapOf(
    "userId" to ValorFirestore.Texto(userId),
    "nombre" to ValorFirestore.Texto(nombre),
    "descripcion" to ValorFirestore.Texto(descripcion),
    "categoria" to ValorFirestore.Texto(categoria),
    "colores" to ValorFirestore.Lista(colores.map(ValorFirestore::Texto)),
    "ocasiones" to ValorFirestore.Lista(ocasiones.map(ValorFirestore::Texto)),
    "fotoUrl" to ValorFirestore.Texto(fotoUrl),
    "etiquetas" to ValorFirestore.Lista(etiquetas.map(ValorFirestore::Texto)),
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
    etiquetas = listaDeTextos("etiquetas"),
)
