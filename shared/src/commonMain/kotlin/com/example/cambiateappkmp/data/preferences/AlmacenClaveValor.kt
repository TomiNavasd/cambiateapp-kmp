package com.example.cambiateappkmp.data.preferences

/** Almacenamiento mínimo de pares clave-valor. Cada plataforma lo guarda a su manera. */
interface AlmacenClaveValor {
    fun leer(clave: String): String?
    fun guardar(clave: String, valor: String)
}

/** Guarda en memoria: se pierde al cerrar la app. Sirve para escritorio, para el fake y para los tests. */
class AlmacenClaveValorEnMemoria : AlmacenClaveValor {
    private val datos = mutableMapOf<String, String>()

    override fun leer(clave: String): String? = datos[clave]

    override fun guardar(clave: String, valor: String) {
        datos[clave] = valor
    }
}
