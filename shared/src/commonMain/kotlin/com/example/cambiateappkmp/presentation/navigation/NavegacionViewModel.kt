package com.example.cambiateappkmp.presentation.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel


class NavegacionViewModel : ViewModel() {

    val pila = mutableStateListOf<Ruta>(Ruta.Login)

    val actual: Ruta get() = pila.last()

    fun ir(ruta: Ruta) {
        pila.add(ruta)
    }

    fun volver(): Boolean {
        if (pila.size <= 1) return false
        pila.removeAt(pila.lastIndex)
        return true
    }

    fun reiniciar(inicio: Ruta = Ruta.Login) {
        pila.clear()
        pila.add(inicio)
    }

    /**
     * Cambia de pestaña sin apilar pantallas: la pila queda en [Armario] o en [Armario, pestaña], como en la nativa
     * (popUpTo del Armario). Si ya estamos en esa pestaña no hace nada. Los ViewModel de cada pestaña siguen vivos,
     * así que conservan su estado (filtros, búsqueda, sugerencias).
     */
    fun irAPestana(pestana: Ruta) {
        require(pestana.esPestana) { "$pestana no es una pestaña" }
        if (actual.mismaPestanaQue(pestana)) return
        pila.clear()
        pila.add(Ruta.Armario)
        if (pestana !is Ruta.Armario) pila.add(pestana)
    }
}
