package com.example.cambiateappkmp.data.preferences

import android.content.SharedPreferences

class AlmacenClaveValorAndroid(
    private val preferencias: SharedPreferences,
) : AlmacenClaveValor {

    override fun leer(clave: String): String? = preferencias.getString(clave, null)

    override fun guardar(clave: String, valor: String) {
        preferencias.edit().putString(clave, valor).apply()
    }
}
