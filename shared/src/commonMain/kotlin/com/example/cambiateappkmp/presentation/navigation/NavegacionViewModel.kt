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

    fun reiniciar() {
        pila.clear()
        pila.add(Ruta.Login)
    }
}