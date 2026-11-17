package com.example.cambiateappkmp.presentation.navigation

import com.example.cambiateappkmp.domain.model.ClimaActual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavegacionTest {

    private fun enElArmario() = NavegacionViewModel().apply { reiniciar(Ruta.Armario) }

    @Test
    fun laBarraSoloSeMuestraEnLasTresPestanas() {
        assertTrue(Ruta.Armario.muestraBarraInferior)
        assertTrue(Ruta.Outfits().muestraBarraInferior)
        assertTrue(Ruta.Mapa.muestraBarraInferior)

        assertFalse(Ruta.Login.muestraBarraInferior)
        assertFalse(Ruta.Registro.muestraBarraInferior)
        assertFalse(Ruta.DetallePrenda("1").muestraBarraInferior)
        assertFalse(Ruta.AltaEdicion(null).muestraBarraInferior)
        assertFalse(Ruta.Ajustes.muestraBarraInferior)
        assertFalse(Ruta.Notificaciones.muestraBarraInferior)
    }

    @Test
    fun outfitsConOSinClimaEsLaMismaPestana() {
        val clima = ClimaActual(12.0, "nublado", llueve = true)
        assertTrue(Ruta.Outfits(clima).mismaPestanaQue(Ruta.Outfits()))
        assertFalse(Ruta.Outfits().mismaPestanaQue(Ruta.Mapa))
        assertFalse(Ruta.Ajustes.mismaPestanaQue(Ruta.Ajustes))
    }

    @Test
    fun cambiarDePestanaNoApilaPantallas() {
        val navegacion = enElArmario()

        navegacion.irAPestana(Ruta.Outfits())
        navegacion.irAPestana(Ruta.Mapa)
        navegacion.irAPestana(Ruta.Outfits())

        assertEquals(listOf(Ruta.Armario, Ruta.Outfits()), navegacion.pila.toList())
    }

    @Test
    fun volverAlArmarioDejaLaPilaEnSuRaiz() {
        val navegacion = enElArmario()
        navegacion.irAPestana(Ruta.Mapa)

        navegacion.irAPestana(Ruta.Armario)

        assertEquals(listOf<Ruta>(Ruta.Armario), navegacion.pila.toList())
    }

    @Test
    fun tocarLaPestanaActualNoHaceNada() {
        val navegacion = enElArmario()
        val clima = ClimaActual(30.0, "soleado", llueve = false)
        navegacion.irAPestana(Ruta.Outfits(clima))

        navegacion.irAPestana(Ruta.Outfits())

        assertEquals(Ruta.Outfits(clima), navegacion.actual)
    }

    @Test
    fun elAtrasDesdeUnaPestanaVuelveAlArmarioYDesdeElArmarioNoHaceNada() {
        val navegacion = enElArmario()
        navegacion.irAPestana(Ruta.Mapa)

        assertTrue(navegacion.volver())
        assertEquals(Ruta.Armario, navegacion.actual)
        assertFalse(navegacion.volver())
        assertEquals(Ruta.Armario, navegacion.actual)
    }

    @Test
    fun sugerirDesdeElMapaVaAOutfitsConElClima() {
        val navegacion = enElArmario()
        navegacion.irAPestana(Ruta.Mapa)
        val clima = ClimaActual(8.0, "lluvia", llueve = true)

        navegacion.irAPestana(Ruta.Outfits(clima))

        assertEquals(Ruta.Outfits(clima), navegacion.actual)
        assertEquals(2, navegacion.pila.size)
    }

    @Test
    fun sugerirSinClimaTambienVaAOutfits() {
        val navegacion = enElArmario()
        navegacion.irAPestana(Ruta.Mapa)

        navegacion.irAPestana(Ruta.Outfits(null))

        assertEquals(Ruta.Outfits(null), navegacion.actual)
    }

    @Test
    fun deAjustesSeVaANotificacionesYElAtrasVuelveDePasoEnPaso() {
        val navegacion = enElArmario()

        navegacion.ir(Ruta.Ajustes)
        navegacion.ir(Ruta.Notificaciones)
        assertEquals(Ruta.Notificaciones, navegacion.actual)

        navegacion.volver()
        assertEquals(Ruta.Ajustes, navegacion.actual)
        navegacion.volver()
        assertEquals(Ruta.Armario, navegacion.actual)
    }

    @Test
    fun elDetalleSeApilaSobreLaPestanaYAlVolverSeRecuperaLaPestana() {
        val navegacion = enElArmario()
        navegacion.irAPestana(Ruta.Mapa)

        navegacion.ir(Ruta.DetallePrenda("7"))
        assertFalse(navegacion.actual.muestraBarraInferior)

        navegacion.volver()
        assertEquals(Ruta.Mapa, navegacion.actual)
    }

    @Test
    fun alReiniciarSeBorraTodaLaPila() {
        val navegacion = enElArmario()
        navegacion.irAPestana(Ruta.Mapa)
        navegacion.ir(Ruta.DetallePrenda("7"))

        navegacion.reiniciar(Ruta.Login)

        assertEquals(listOf<Ruta>(Ruta.Login), navegacion.pila.toList())
    }

    @Test
    fun noSePuedeIrComoPestanaAUnaPantallaQueNoLoEs() {
        val navegacion = enElArmario()

        assertFailsWith<IllegalArgumentException> { navegacion.irAPestana(Ruta.Ajustes) }
    }
}
