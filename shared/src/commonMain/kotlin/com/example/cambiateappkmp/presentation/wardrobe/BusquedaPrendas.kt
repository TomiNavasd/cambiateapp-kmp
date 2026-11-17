package com.example.cambiateappkmp.presentation.wardrobe

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Prenda

private const val CON_TILDE = "áàäâãéèëêíìïîóòöôõúùüûñç"
private const val SIN_TILDE = "aaaaaeeeeiiiiooooouuuunc"

/** Minúsculas y sin tildes, para comparar sin importar cómo se escribió. */
fun normalizarBusqueda(texto: String): String = buildString {
    for (c in texto.trim().lowercase()) {
        val i = CON_TILDE.indexOf(c)
        append(if (i >= 0) SIN_TILDE[i] else c)
    }
}

/** Una prenda coincide si la consulta aparece en su nombre, en el nombre de su categoría, en alguno de sus colores o en alguna de sus etiquetas (que el usuario no ve). */
fun Prenda.coincideConBusqueda(consulta: String): Boolean {
    val buscado = normalizarBusqueda(consulta)
    if (buscado.isEmpty()) return true
    val campos = listOf(nombre, categoria.etiqueta()) + colores + etiquetas
    return campos.any { normalizarBusqueda(it).contains(buscado) }
}

/** Primero el filtro por categoría y después el texto. */
fun List<Prenda>.filtrarArmario(categoria: Categoria?, consulta: String): List<Prenda> =
    filter { categoria == null || it.categoria == categoria }
        .filter { it.coincideConBusqueda(consulta) }
