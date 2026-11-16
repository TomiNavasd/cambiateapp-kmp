package com.example.cambiateappkmp.data.preferences

import java.util.prefs.BackingStoreException
import java.util.prefs.Preferences

/**
 * Almacén de escritorio con java.util.prefs: lo guardado (tema, recordatorios, última ubicación)
 * queda entre ejecuciones. En Windows va al registro, dentro del usuario actual.
 */
internal class AlmacenClaveValorPrefs(
    private val nodo: Preferences = Preferences.userRoot().node("com/example/cambiateappkmp/preferencias"),
) : AlmacenClaveValor {

    override fun leer(clave: String): String? = nodo.get(clave, null)

    override fun guardar(clave: String, valor: String) {
        nodo.put(clave, valor)
        try {
            nodo.flush()
        } catch (e: BackingStoreException) {
            // Si no se pudo escribir en disco, el valor sigue valiendo mientras la app esté abierta.
        }
    }
}
