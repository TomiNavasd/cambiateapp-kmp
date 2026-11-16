package com.example.cambiateappkmp.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RutasSemana3Test {

    private fun navegacionEnElArmario() = NavegacionViewModel().apply { reiniciar(Ruta.Armario) }

    @Test
    fun delArmarioSeVaAlMapaYElAtrasVuelveAlArmario() {
        val navegacion = navegacionEnElArmario()

        navegacion.ir(RutaMapa)
        assertEquals(RutaMapa, navegacion.actual)

        assertTrue(navegacion.volver())
        assertEquals(Ruta.Armario, navegacion.actual)
    }

    @Test
    fun deAjustesSeVaANotificacionesYElAtrasVuelveAAjustesYDespuesAlArmario() {
        val navegacion = navegacionEnElArmario()

        navegacion.ir(RutaAjustes)
        navegacion.ir(RutaNotificaciones)
        assertEquals(RutaNotificaciones, navegacion.actual)

        navegacion.volver()
        assertEquals(RutaAjustes, navegacion.actual)

        navegacion.volver()
        assertEquals(Ruta.Armario, navegacion.actual)
    }

    @Test
    fun desdeElArmarioYaNoHayAdondeVolver() {
        val navegacion = navegacionEnElArmario()

        assertFalse(navegacion.volver())
        assertEquals(Ruta.Armario, navegacion.actual)
    }

    @Test
    fun alReiniciarLaNavegacionSeBorranLasPantallasDeLaSemana3() {
        val navegacion = navegacionEnElArmario()
        navegacion.ir(RutaAjustes)
        navegacion.ir(RutaNotificaciones)

        // Por ejemplo, al cerrar sesión la navegación vuelve al inicio.
        navegacion.reiniciar(Ruta.Login)

        assertEquals(listOf<Ruta>(Ruta.Login), navegacion.pila.toList())
    }
}
