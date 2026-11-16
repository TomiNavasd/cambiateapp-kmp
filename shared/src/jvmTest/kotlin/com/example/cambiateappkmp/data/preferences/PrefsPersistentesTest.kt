package com.example.cambiateappkmp.data.preferences

import com.example.cambiateappkmp.data.rest.AlmacenSesionPrefs
import com.example.cambiateappkmp.data.rest.SesionFirebase
import com.example.cambiateappkmp.domain.model.TemaApp
import com.example.cambiateappkmp.domain.model.Ubicacion
import com.example.cambiateappkmp.data.repository.PreferenciasRepositoryImpl
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/** Usan un nodo temporal de java.util.prefs (distinto del real de la app) y lo borran al terminar. */
class PrefsPersistentesTest {

    private val nodos = mutableListOf<Preferences>()

    private fun nodoTemporal(): Preferences =
        Preferences.userRoot().node("com/example/cambiateappkmp/test-${UUID.randomUUID()}").also { nodos += it }

    @AfterTest
    fun limpiar() {
        nodos.forEach {
            try {
                it.removeNode()
                it.parent().flush()
            } catch (e: Exception) {
                // Si no se puede borrar, no afecta al resultado del test.
            }
        }
    }

    @Test
    fun loGuardadoSeLeeAunConUnAlmacenNuevo() {
        val nodo = nodoTemporal()
        AlmacenClaveValorPrefs(nodo).guardar("tema", "OSCURO")

        assertEquals("OSCURO", AlmacenClaveValorPrefs(nodo).leer("tema"))
        assertNull(AlmacenClaveValorPrefs(nodo).leer("no-existe"))
    }

    @Test
    fun lasPreferenciasSobreviven_alReabrirLaApp() = runTest {
        val nodo = nodoTemporal()
        val primera = PreferenciasRepositoryImpl(AlmacenClaveValorPrefs(nodo))
        primera.guardarTema(TemaApp.OSCURO)
        primera.guardarUltimaUbicacion(Ubicacion(-34.6, -58.4))

        val reabierta = PreferenciasRepositoryImpl(AlmacenClaveValorPrefs(nodo))

        assertEquals(TemaApp.OSCURO, reabierta.tema.first())
        assertEquals(-34.6, reabierta.ultimaUbicacion()?.lat)
    }

    @Test
    fun laSesionSeGuardaSeLeeYSeBorra() {
        val nodo = nodoTemporal()
        val sesion = SesionFirebase("uid1", "ana@test.com", "T1", "R1", expiraEnMs = 12345L)
        val almacen = AlmacenSesionPrefs(nodo)
        assertNull(almacen.leer())

        almacen.guardar(sesion)
        assertEquals(sesion, AlmacenSesionPrefs(nodo).leer())

        almacen.borrar()
        assertNull(AlmacenSesionPrefs(nodo).leer())
    }

    @Test
    fun unaSesionSinRefreshTokenSeConsideraInexistente() {
        val nodo = nodoTemporal()
        nodo.put("uid", "uid1")

        assertNull(AlmacenSesionPrefs(nodo).leer())
    }
}
