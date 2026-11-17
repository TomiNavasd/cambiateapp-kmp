package com.example.cambiateappkmp.domain.model

/** Las cuatro temporadas, en el orden en que se muestran. [clave] es la etiqueta de la prenda (sin tildes). */
enum class Temporada(val clave: String, val nombre: String) {
    PRIMAVERA("primavera", "Primavera"),
    VERANO("verano", "Verano"),
    OTONO("otono", "Otoño"),
    INVIERNO("invierno", "Invierno"),
}

/** Nombre para mostrar de un outfit: "{superior} con {inferior}", solo una de las dos, o "Outfit". */
fun List<Prenda>.nombreVisibleDeOutfit(): String {
    val superior = firstOrNull { it.categoria == Categoria.SUPERIOR }?.nombre
    val inferior = firstOrNull { it.categoria == Categoria.INFERIOR }?.nombre
    return when {
        superior != null && inferior != null -> "$superior con $inferior"
        superior != null -> superior
        inferior != null -> inferior
        else -> "Outfit"
    }
}

/**
 * Temporadas que comparten todas las prendas que tienen alguna etiqueta de temporada.
 * Las prendas sin etiquetas de temporada no cuentan. Vacía si no hay datos o si no coinciden en ninguna.
 */
fun List<Prenda>.temporadasComunes(): List<Temporada> {
    val conjuntos = map { prenda -> Temporada.entries.filter { it.clave in prenda.etiquetas }.toSet() }
        .filter { it.isNotEmpty() }
    if (conjuntos.isEmpty()) return emptyList()

    val comunes = conjuntos.reduce { a, b -> a intersect b }
    return Temporada.entries.filter { it in comunes }
}

/** "Todo el año" si son las cuatro, los nombres separados por " / " en otro caso, y null si no hay ninguna. */
fun textoDeTemporadas(temporadas: List<Temporada>): String? = when {
    temporadas.isEmpty() -> null
    temporadas.size == Temporada.entries.size -> "Todo el año"
    else -> temporadas.joinToString(" / ") { it.nombre }
}

/** Atajo: el texto de temporadas de un outfit a partir de sus prendas. */
fun List<Prenda>.temporadaTextoDeOutfit(): String? = textoDeTemporadas(temporadasComunes())
