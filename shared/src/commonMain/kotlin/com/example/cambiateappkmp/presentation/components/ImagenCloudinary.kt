package com.example.cambiateappkmp.presentation.components

private const val MARCA_UPLOAD = "/upload/"

// Una transformación de Cloudinary son pares "letras_valor" separados por comas, por ejemplo "w_400,h_400,c_fill".
private val TRANSFORMACION = Regex("^[a-z]{1,3}_[^,/]+(,[a-z]{1,3}_[^,/]+)*$")
private val VERSION = Regex("^v\\d+$")

/**
 * Devuelve la URL de una foto de Cloudinary pidiéndole la imagen ya redimensionada, para mostrarla nítida y liviana.
 * Es solo para mostrar: la URL guardada en Firestore no cambia.
 *
 * - Si la URL no es de Cloudinary (no tiene "/upload/"), la devuelve igual.
 * - Si ya trae transformaciones, la devuelve igual (no las duplica).
 * - [recortar] = true: cuadrada de [ancho] x [ancho] (`w_,h_,c_fill`), para las grillas.
 *   [recortar] = false: de hasta [ancho] de ancho, sin recortar y con las mismas proporciones (`w_,c_limit`), para
 *   mostrar la foto entera.
 * - En los dos casos pide calidad y formato automáticos (`q_auto,f_auto`).
 */
fun String.conTamanioCloudinary(ancho: Int, recortar: Boolean = true): String {
    val inicio = indexOf(MARCA_UPLOAD)
    if (inicio < 0 || ancho <= 0) return this

    val despues = inicio + MARCA_UPLOAD.length
    val primerTramo = substring(despues).substringBefore('/')
    // Si es la versión (v1234) o parte de la ruta, todavía no hay transformaciones. Si parece una, no se toca.
    if (!VERSION.matches(primerTramo) && TRANSFORMACION.matches(primerTramo)) return this

    val transformacion = if (recortar) {
        "w_$ancho,h_$ancho,c_fill,q_auto,f_auto"
    } else {
        "w_$ancho,c_limit,q_auto,f_auto"
    }
    return substring(0, despues) + transformacion + "/" + substring(despues)
}
