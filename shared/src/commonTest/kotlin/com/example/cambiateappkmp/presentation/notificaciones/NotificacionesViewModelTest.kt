package com.example.cambiateappkmp.presentation.notificaciones

import com.example.cambiateappkmp.data.fake.FakeClimaRepository
import com.example.cambiateappkmp.data.fake.FakePreferenciasRepository
import com.example.cambiateappkmp.data.fake.FakeUbicacionProvider
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.error.UbicacionException
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.ConfigNotificaciones
import com.example.cambiateappkmp.domain.model.PronosticoManiana
import com.example.cambiateappkmp.domain.model.Ubicacion
import com.example.cambiateappkmp.domain.notification.Notificador
import com.example.cambiateappkmp.domain.repository.ClimaRepository
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** Notificador que solo anota lo que le piden. */
private class NotificadorGrabador : Notificador {
    val programadas = mutableListOf<Pair<Int, Int>>()
    var cancelaciones = 0
    val mostradas = mutableListOf<Pair<String, String>>()

    override fun programarDiaria(hora: Int, minuto: Int) {
        programadas += hora to minuto
    }

    override fun cancelarDiaria() {
        cancelaciones++
    }

    override fun mostrarAhora(titulo: String, mensaje: String) {
        mostradas += titulo to mensaje
    }
}

private class ClimaQueFalla : ClimaRepository {
    override suspend fun obtenerClimaActual(lat: Double, lng: Double): Result<ClimaActual> =
        Result.failure(DomainException.SinConexion)

    override suspend fun obtenerPronosticoManiana(lat: Double, lng: Double): Result<PronosticoManiana> =
        Result.failure(DomainException.SinConexion)
}

private class UbicacionQueFalla : UbicacionProvider {
    override suspend fun obtenerUbicacionActual(): Result<Ubicacion> =
        Result.failure(UbicacionException.SinPermiso())
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificacionesViewModelTest {

    private val notificador = NotificadorGrabador()
    private val preferencias = FakePreferenciasRepository()

    @BeforeTest
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun despues() {
        Dispatchers.resetMain()
    }

    private fun TestScope.crear(
        clima: ClimaRepository = FakeClimaRepository(),
        ubicacion: UbicacionProvider = FakeUbicacionProvider(),
    ): NotificacionesViewModel {
        val viewModel = NotificacionesViewModel(preferencias, notificador, clima, ubicacion)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect { } }
        return viewModel
    }

    @Test
    fun activarGuardaLaConfiguracionYProgramaLaTareaALasVeinte() = runTest {
        val viewModel = crear()

        viewModel.activar()

        assertEquals(ConfigNotificaciones(true, 20, 0), preferencias.notificaciones.first())
        assertEquals(listOf(20 to 0), notificador.programadas)
        assertEquals(true, viewModel.estado.value.activadas)
    }

    @Test
    fun desactivarGuardaYCancelaLaTarea() = runTest {
        val viewModel = crear()
        viewModel.activar()

        viewModel.desactivar()

        assertEquals(false, preferencias.notificaciones.first().activadas)
        assertEquals(1, notificador.cancelaciones)
    }

    @Test
    fun cambiarLaHoraConElRecordatorioActivoReprogramaLaTarea() = runTest {
        val viewModel = crear()
        viewModel.activar()

        viewModel.elegirHora(8, 30)

        assertEquals(ConfigNotificaciones(true, 8, 30), preferencias.notificaciones.first())
        assertEquals(listOf(20 to 0, 8 to 30), notificador.programadas)
    }

    @Test
    fun cambiarLaHoraConElRecordatorioApagadoSoloLaGuarda() = runTest {
        val viewModel = crear()

        viewModel.elegirHora(7, 15)

        assertEquals(ConfigNotificaciones(false, 7, 15), preferencias.notificaciones.first())
        assertEquals(emptyList(), notificador.programadas)
        assertEquals(1, notificador.cancelaciones)
    }

    @Test
    fun laPruebaMuestraElMensajeArmadoConElPronosticoDeMananaDeEjemplo() = runTest {
        val viewModel = crear()

        viewModel.enviarPrueba()
        advanceUntilIdle()

        // El clima de ejemplo trae 70 % de lluvia para mañana.
        assertEquals("Mañana llueve", notificador.mostradas.single().first)
        assertEquals("Listo, mirá la barra de notificaciones", viewModel.estado.value.aviso)
    }

    @Test
    fun laPruebaUsaLaUltimaUbicacionGuardadaYNoPideElGps() = runTest {
        preferencias.guardarUltimaUbicacion(Ubicacion(-31.4, -64.2))
        val viewModel = crear(ubicacion = UbicacionQueFalla())

        viewModel.enviarPrueba()
        advanceUntilIdle()

        assertEquals("Mañana llueve", notificador.mostradas.single().first)
    }

    @Test
    fun siFallaElClimaLaPruebaMandaElAvisoGeneral() = runTest {
        val viewModel = crear(clima = ClimaQueFalla())

        viewModel.enviarPrueba()
        advanceUntilIdle()

        assertEquals("Mirá el clima de mañana", notificador.mostradas.single().first)
        assertEquals("No pudimos obtener el clima: te mandamos el aviso general", viewModel.estado.value.aviso)
    }

    @Test
    fun siNoHayUbicacionLaPruebaMandaElAvisoGeneral() = runTest {
        val viewModel = crear(ubicacion = UbicacionQueFalla())

        viewModel.enviarPrueba()
        advanceUntilIdle()

        assertEquals("Mirá el clima de mañana", notificador.mostradas.single().first)
    }

    @Test
    fun actualizarLaUbicacionLaDejaGuardada() = runTest {
        val viewModel = crear()

        viewModel.actualizarUbicacion()
        advanceUntilIdle()

        assertEquals(Ubicacion(lat = -34.6037, lng = -58.3816), preferencias.ultimaUbicacion())
    }

    @Test
    fun siNoSePuedeObtenerLaUbicacionAvisaYNoGuardaNada() = runTest {
        val viewModel = crear(ubicacion = UbicacionQueFalla())

        viewModel.actualizarUbicacion()
        advanceUntilIdle()

        assertNull(preferencias.ultimaUbicacion())
        assertEquals("No pudimos obtener tu ubicación. Revisá que el GPS esté activado.", viewModel.estado.value.aviso)
    }

    @Test
    fun losAvisosDePermisoRechazadoSeMuestranYSePuedenBorrar() = runTest {
        val viewModel = crear()

        viewModel.permisoNotificacionesRechazado()
        assertEquals("Sin permiso no podemos avisarte. Activalo en los ajustes del sistema.", viewModel.estado.value.aviso)

        viewModel.avisoMostrado()
        assertNull(viewModel.estado.value.aviso)
    }

    @Test
    fun laHoraSeFormateaConDosDigitos() {
        assertEquals("08:05", formatearHora(8, 5))
        assertEquals("20:00", formatearHora(20, 0))
    }
}
