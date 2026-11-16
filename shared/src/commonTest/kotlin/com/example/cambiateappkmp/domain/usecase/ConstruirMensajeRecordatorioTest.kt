package com.example.cambiateappkmp.domain.usecase

import com.example.cambiateappkmp.domain.model.PronosticoManiana
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstruirMensajeRecordatorioTest {

    private fun pronostico(minima: Double, maxima: Double, lluvia: Int) = PronosticoManiana(
        maximaC = maxima,
        minimaC = minima,
        probabilidadLluvia = lluvia,
        descripcion = "Sin importancia",
    )

    @Test
    fun conLluviaAvisaQueLlueveYMuestraProbabilidadYTemperaturas() {
        val (titulo, mensaje) = construirMensajeRecordatorio(pronostico(minima = 14.0, maxima = 22.0, lluvia = 60))

        assertEquals("Mañana llueve", titulo)
        assertContains(mensaje, "60%")
        assertContains(mensaje, "14°")
        assertContains(mensaje, "22°")
        assertContains(mensaje, "impermeable")
    }

    @Test
    fun conCincuentaPorCientoDeLluviaYaAvisaQueLluevePeroConCuarentaYNueveNo() {
        assertEquals("Mañana llueve", construirMensajeRecordatorio(pronostico(14.0, 22.0, 50)).first)
        assertEquals("Mañana, clima agradable", construirMensajeRecordatorio(pronostico(14.0, 22.0, 49)).first)
    }

    @Test
    fun conFrioAvisaLaMinima() {
        val (titulo, mensaje) = construirMensajeRecordatorio(pronostico(minima = 8.0, maxima = 18.0, lluvia = 10))

        assertEquals("Mañana hace frío", titulo)
        assertContains(mensaje, "8°")
        assertContains(mensaje, "abrigate")
    }

    @Test
    fun conMinimaDeDoceGradosJustosNoEsFrio() {
        assertEquals("Mañana, clima agradable", construirMensajeRecordatorio(pronostico(12.0, 20.0, 0)).first)
        assertEquals("Mañana hace frío", construirMensajeRecordatorio(pronostico(11.9, 20.0, 0)).first)
    }

    @Test
    fun conCalorAvisaLaMaxima() {
        val (titulo, mensaje) = construirMensajeRecordatorio(pronostico(minima = 20.0, maxima = 31.0, lluvia = 0))

        assertEquals("Mañana hace calor", titulo)
        assertContains(mensaje, "31°")
        assertContains(mensaje, "liviana")
    }

    @Test
    fun conMaximaDeVeintiseisGradosJustosYaEsCalorPeroConVeinticincoNueveNo() {
        assertEquals("Mañana hace calor", construirMensajeRecordatorio(pronostico(18.0, 26.0, 0)).first)
        assertEquals("Mañana, clima agradable", construirMensajeRecordatorio(pronostico(18.0, 25.9, 0)).first)
    }

    @Test
    fun sinLluviaNiFrioNiCalorEsAgradableYMuestraLasTemperaturas() {
        val (titulo, mensaje) = construirMensajeRecordatorio(pronostico(minima = 14.0, maxima = 22.0, lluvia = 10))

        assertEquals("Mañana, clima agradable", titulo)
        assertContains(mensaje, "14°")
        assertContains(mensaje, "22°")
    }

    @Test
    fun laLluviaTienePrioridadSobreElFrio() {
        assertEquals("Mañana llueve", construirMensajeRecordatorio(pronostico(minima = 5.0, maxima = 12.0, lluvia = 80)).first)
    }

    @Test
    fun laLluviaTienePrioridadSobreElCalor() {
        assertEquals("Mañana llueve", construirMensajeRecordatorio(pronostico(minima = 24.0, maxima = 33.0, lluvia = 70)).first)
    }

    @Test
    fun elFrioTienePrioridadSobreElCalor() {
        // Un día de mucha amplitud: arranca helado y termina caluroso.
        assertEquals("Mañana hace frío", construirMensajeRecordatorio(pronostico(minima = 8.0, maxima = 30.0, lluvia = 0)).first)
    }

    @Test
    fun laTemperaturaSeRedondeaAlEntero() {
        val (_, mensaje) = construirMensajeRecordatorio(pronostico(minima = 8.6, maxima = 18.0, lluvia = 0))

        assertContains(mensaje, "9°")
    }

    @Test
    fun elMensajeGeneralNoEstaVacio() {
        assertEquals("Mirá el clima de mañana", MENSAJE_RECORDATORIO_GENERAL.first)
        assertTrue(MENSAJE_RECORDATORIO_GENERAL.second.isNotBlank())
    }
}
