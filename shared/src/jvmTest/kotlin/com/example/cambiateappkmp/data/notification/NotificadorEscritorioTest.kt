package com.example.cambiateappkmp.data.notification

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.PronosticoManiana
import com.example.cambiateappkmp.domain.model.Ubicacion
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import com.example.cambiateappkmp.domain.usecase.MENSAJE_RECORDATORIO_GENERAL
import com.example.cambiateappkmp.domain.usecase.construirMensajeRecordatorio
import com.example.cambiateappkmp.presentation.avisos.AvisoEnApp
import com.example.cambiateappkmp.presentation.avisos.AvisosEnApp
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private val ZONA = ZoneId.of("America/Argentina/Buenos_Aires")

private fun fecha(dia: Int, hora: Int, minuto: Int, segundo: Int = 0): ZonedDateTime =
    ZonedDateTime.of(2026, 11, dia, hora, minuto, segundo, 0, ZONA)

private class BandejaDePrueba(var funciona: Boolean = true) : BandejaDelSistema {
    val mostradas = mutableListOf<Pair<String, String>>()
    override fun mostrar(titulo: String, mensaje: String): Boolean {
        if (funciona) mostradas += titulo to mensaje
        return funciona
    }
}

private class AvisosDePrueba : AvisosEnApp {
    val recibidos = mutableListOf<AvisoEnApp>()
    override val avisos: Flow<AvisoEnApp> = emptyFlow()
    override fun mostrar(titulo: String, mensaje: String) { recibidos += AvisoEnApp(titulo, mensaje) }
}

private class ClimaDePrueba(var pronostico: Result<PronosticoManiana>) : ClimaRepository {
    var consultas = 0
    override suspend fun obtenerClimaActual(lat: Double, lng: Double): Result<ClimaActual> =
        Result.failure(UnsupportedOperationException())

    override suspend fun obtenerPronosticoManiana(lat: Double, lng: Double): Result<PronosticoManiana> {
        consultas++
        return pronostico
    }
}

private class UbicacionDePrueba : UbicacionProvider {
    override suspend fun obtenerUbicacionActual() = Result.success(Ubicacion(-34.6, -58.4, aproximada = true))
}

class MilisHastaProximaEjecucionTest {

    @Test
    fun siLaHoraTodaviaNoPasoEsHoy() {
        // 18:00 -> 20:00 son 2 horas.
        assertEquals(2 * 60 * 60 * 1000L, milisHastaProximaEjecucion(fecha(16, 18, 0), 20, 0))
    }

    @Test
    fun siLaHoraYaPasoEsManiana() {
        // 21:00 -> 20:00 del día siguiente son 23 horas.
        assertEquals(23 * 60 * 60 * 1000L, milisHastaProximaEjecucion(fecha(16, 21, 0), 20, 0))
    }

    @Test
    fun justoEnLaHoraExactaSeProgramaParaManiana() {
        assertEquals(24 * 60 * 60 * 1000L, milisHastaProximaEjecucion(fecha(16, 20, 0), 20, 0))
    }

    @Test
    fun tieneEnCuentaLosSegundosYLosMinutos() {
        // 19:59:30 -> 20:00:00 son 30 segundos.
        assertEquals(30_000L, milisHastaProximaEjecucion(fecha(16, 19, 59, 30), 20, 0))
        assertEquals(45 * 60 * 1000L, milisHastaProximaEjecucion(fecha(16, 8, 0), 8, 45))
    }

    @Test
    fun alCruzarElFinDeMesPasaAlDiaSiguiente() {
        val finDeMes = ZonedDateTime.of(2026, 11, 30, 23, 0, 0, 0, ZONA)

        assertEquals(60 * 60 * 1000L, milisHastaProximaEjecucion(finDeMes, 0, 0))
    }

    @Test
    fun enUnCambioDeHoraLaDiferenciaSigueElRelojDeLaPared() {
        // Europa pasa a horario de verano el 29/03/2026 a las 02:00: esa noche tiene 23 horas reales.
        val madrid = ZoneId.of("Europe/Madrid")
        val antes = ZonedDateTime.of(2026, 3, 28, 23, 0, 0, 0, madrid)

        assertEquals(8 * 60 * 60 * 1000L, milisHastaProximaEjecucion(antes, 8, 0))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificadorEscritorioTest {

    private val bandeja = BandejaDePrueba()
    private val avisos = AvisosDePrueba()
    private val frio = PronosticoManiana(maximaC = 12.0, minimaC = 4.0, probabilidadLluvia = 0, descripcion = "frío")
    private val clima = ClimaDePrueba(Result.success(frio))

    private fun notificador(alcance: CoroutineScope, ahora: ZonedDateTime) = NotificadorEscritorio(
        clima = clima,
        ubicacion = UbicacionDePrueba(),
        avisos = avisos,
        bandeja = bandeja,
        alcance = alcance,
        ahora = { ahora },
    )

    @Test
    fun mostrarAhoraUsaLaBandejaDelSistema() = runTest {
        val n = notificador(backgroundScope, fecha(16, 18, 0))

        n.mostrarAhora("Hola", "Mundo")

        assertEquals(listOf("Hola" to "Mundo"), bandeja.mostradas)
        assertTrue(avisos.recibidos.isEmpty())
    }

    @Test
    fun siLaBandejaNoFuncionaElAvisoSaleDentroDeLaApp() = runTest {
        bandeja.funciona = false
        val n = notificador(backgroundScope, fecha(16, 18, 0))

        n.mostrarAhora("Hola", "Mundo")

        assertEquals(listOf(AvisoEnApp("Hola", "Mundo")), avisos.recibidos)
    }

    @Test
    fun elRecordatorioSaleALaHoraProgramadaConElMensajeDelClima() = runTest {
        val n = notificador(backgroundScope, fecha(16, 18, 0))

        n.programarDiaria(20, 0)
        advanceTimeBy(2 * 60 * 60 * 1000L - 1)
        runCurrent()
        assertTrue(bandeja.mostradas.isEmpty(), "todavía no es la hora")

        advanceTimeBy(2)
        runCurrent()

        assertEquals(listOf(construirMensajeRecordatorio(frio)), bandeja.mostradas)
        assertEquals(1, clima.consultas)
    }

    @Test
    fun siElClimaFallaIgualSaleElAvisoGeneral() = runTest {
        clima.pronostico = Result.failure(DomainException.SinConexion)
        val n = notificador(backgroundScope, fecha(16, 18, 0))

        n.programarDiaria(20, 0)
        advanceTimeBy(2 * 60 * 60 * 1000L + 1)
        runCurrent()

        assertEquals(listOf(MENSAJE_RECORDATORIO_GENERAL), bandeja.mostradas)
    }

    @Test
    fun cancelarEvitaQueSalgaElAviso() = runTest {
        val n = notificador(backgroundScope, fecha(16, 18, 0))

        n.programarDiaria(20, 0)
        n.cancelarDiaria()
        advanceTimeBy(3 * 60 * 60 * 1000L)
        runCurrent()

        assertTrue(bandeja.mostradas.isEmpty())
        assertEquals(0, clima.consultas)
    }

    @Test
    fun reprogramarReemplazaLaHoraAnterior() = runTest {
        val n = notificador(backgroundScope, fecha(16, 18, 0))

        n.programarDiaria(19, 0)
        n.programarDiaria(20, 0)
        advanceTimeBy(60 * 60 * 1000L + 1) // pasó la hora vieja (19:00)
        runCurrent()
        assertTrue(bandeja.mostradas.isEmpty())

        advanceTimeBy(60 * 60 * 1000L) // llegó la nueva (20:00)
        runCurrent()
        assertEquals(1, bandeja.mostradas.size)
    }
}
