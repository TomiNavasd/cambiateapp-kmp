package com.example.cambiateappkmp.presentation.outfits

import com.example.cambiateappkmp.data.fake.FakeOutfitsRepository
import com.example.cambiateappkmp.data.fake.FakePrendaRepository
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.OutfitSuggester
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import com.example.cambiateappkmp.domain.usecase.ObserveOutfitsUseCase
import com.example.cambiateappkmp.domain.usecase.ObservePrendasUseCase
import com.example.cambiateappkmp.domain.usecase.SaveOutfitUseCase
import com.example.cambiateappkmp.domain.usecase.SuggestOutfitsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private class SugeridorDePrueba(var resultado: Result<List<Outfit>>) : OutfitSuggester {
    var ocasionRecibida: Ocasion? = null
    var climaRecibido: ClimaActual? = null
    var llamadas = 0
    override suspend fun sugerir(ocasion: Ocasion, prendas: List<Prenda>, clima: ClimaActual?): Result<List<Outfit>> {
        llamadas++
        ocasionRecibida = ocasion
        climaRecibido = clima
        return resultado
    }
}

/** Un repositorio de outfits de verdad (el fake) al que se le pueden hacer fallar las escrituras. */
private class OutfitsConFallos(
    private val base: FakeOutfitsRepository,
    var error: Throwable? = null,
) : OutfitsRepository by base {
    override suspend fun guardarOutfit(outfit: Outfit): Result<Unit> =
        error?.let { Result.failure(it) } ?: base.guardarOutfit(outfit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class OutfitsViewModelTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private val fecha = Instant.fromEpochSeconds(1_763_000_000)

    // Los ejemplos del fake: o1 = casual [1,4,6], o2 = trabajo [2,5,7,9], o3 = salida [3,4,6,8,10].
    private val nuevo = Outfit("", Ocasion.CASUAL, listOf("1", "4", "7"), fecha, false)
    private val repetido = Outfit("", Ocasion.CASUAL, listOf("6", "1", "4"), fecha, false)

    private class Armado(
        val vm: OutfitsViewModel,
        val sugeridor: SugeridorDePrueba,
        val outfits: OutfitsConFallos,
    )

    private fun armar(
        t: TestScope,
        sugerencias: Result<List<Outfit>> = Result.success(listOf(nuevo)),
        outfits: FakeOutfitsRepository = FakeOutfitsRepository(),
        prendas: FakePrendaRepository = FakePrendaRepository(),
    ): Armado {
        val conFallos = OutfitsConFallos(outfits)
        val sugeridor = SugeridorDePrueba(sugerencias)
        val observePrendas = ObservePrendasUseCase(prendas)
        val vm = OutfitsViewModel(
            observeOutfits = ObserveOutfitsUseCase(conFallos),
            observePrendas = observePrendas,
            saveOutfit = SaveOutfitUseCase(conFallos),
            suggestOutfits = SuggestOutfitsUseCase(observePrendas, sugeridor),
        )
        t.backgroundScope.launch(UnconfinedTestDispatcher(t.testScheduler)) { vm.uiState.collect { } }
        return Armado(vm, sugeridor, conFallos)
    }

    private fun OutfitsViewModel.contenido() = assertIs<OutfitsUiState.Contenido>(uiState.value)

    @Test
    fun alAbrirMuestraLosGuardadosConSusPrendasYLaOcasionCasual() = runTest {
        val a = armar(this)

        val estado = a.vm.contenido()

        assertEquals(Ocasion.CASUAL, estado.ocasion)
        assertEquals(emptyList(), estado.sugerencias)
        assertEquals(3, estado.guardados.size)
        assertEquals(listOf(1, 4, 6).map { it.toString() }, estado.guardados.first { it.outfit.id == "o1" }.prendas.map { it.id })
        assertNull(estado.aviso)
    }

    @Test
    fun sugerirMuestraLasSugerenciasConPrendasCompletasYPasaLaOcasion() = runTest {
        val a = armar(this)

        a.vm.onEvent(OutfitsUiEvent.OcasionSeleccionada(Ocasion.TRABAJO))
        a.vm.onEvent(OutfitsUiEvent.Sugerir)

        val estado = a.vm.contenido()
        assertEquals(Ocasion.TRABAJO, a.sugeridor.ocasionRecibida)
        assertEquals(1, estado.sugerencias.size)
        assertEquals(listOf("1", "4", "7"), estado.sugerencias.single().prendas.map { it.id })
        assertEquals(false, estado.sugiriendo)
        assertNull(estado.aviso)
    }

    @Test
    fun cambiarDeOcasionLimpiaLasSugerencias() = runTest {
        val a = armar(this)
        a.vm.onEvent(OutfitsUiEvent.Sugerir)

        a.vm.onEvent(OutfitsUiEvent.OcasionSeleccionada(Ocasion.SALIDA))

        assertEquals(emptyList(), a.vm.contenido().sugerencias)
        assertEquals(Ocasion.SALIDA, a.vm.contenido().ocasion)
    }

    @Test
    fun sinSugerenciasAvisaQueFaltanPrendas() = runTest {
        val a = armar(this, sugerencias = Result.success(emptyList()))

        a.vm.onEvent(OutfitsUiEvent.Sugerir)

        assertEquals(OutfitsTextos.SIN_SUGERENCIAS, a.vm.contenido().aviso)
        assertTrue(OutfitsTextos.SIN_SUGERENCIAS.startsWith("No hay prendas suficientes para esa ocasión"))
        assertEquals(emptyList(), a.vm.contenido().sugerencias)
    }

    @Test
    fun loserroresAlSugerirSeMuestranComoAviso() = runTest {
        val casos = listOf(
            DomainException.SinConexion to OutfitsTextos.SIN_CONEXION,
            DomainException.SinPermisos to OutfitsTextos.SIN_PERMISOS,
            DomainException.SesionNoIniciada to OutfitsTextos.SESION_VENCIDA,
            DomainException.Desconocido(IllegalStateException("x")) to OutfitsTextos.INESPERADO,
        )
        for ((error, mensaje) in casos) {
            val a = armar(this, sugerencias = Result.failure(error))

            a.vm.onEvent(OutfitsUiEvent.Sugerir)

            assertEquals(mensaje, a.vm.contenido().aviso, "con $error")
            assertEquals(false, a.vm.contenido().sugiriendo)
        }
    }

    @Test
    fun guardarUnaSugerenciaLaPasaAGuardadosYAvisa() = runTest {
        val a = armar(this)
        a.vm.onEvent(OutfitsUiEvent.Sugerir)

        a.vm.onEvent(OutfitsUiEvent.GuardarSugerencia(0))

        val estado = a.vm.contenido()
        assertEquals(emptyList(), estado.sugerencias)
        assertEquals(4, estado.guardados.size)
        assertEquals(OutfitsTextos.GUARDADO, estado.aviso)
    }

    @Test
    fun unOutfitDuplicadoNoSeGuardaYAvisaQueYaEstaGuardado() = runTest {
        val a = armar(this, sugerencias = Result.success(listOf(repetido)))
        a.vm.onEvent(OutfitsUiEvent.Sugerir)

        a.vm.onEvent(OutfitsUiEvent.GuardarSugerencia(0))

        val estado = a.vm.contenido()
        assertEquals("Ese outfit ya está guardado", estado.aviso)
        assertEquals(emptyList(), estado.sugerencias, "la sugerencia repetida se quita")
        assertEquals(3, estado.guardados.size)
    }

    @Test
    fun siFallaAlGuardarLaSugerenciaSeConservaYSeAvisa() = runTest {
        val a = armar(this)
        a.vm.onEvent(OutfitsUiEvent.Sugerir)
        a.outfits.error = DomainException.SinConexion

        a.vm.onEvent(OutfitsUiEvent.GuardarSugerencia(0))

        val estado = a.vm.contenido()
        assertEquals(1, estado.sugerencias.size)
        assertEquals(OutfitsTextos.SIN_CONEXION, estado.aviso)
        assertEquals(3, estado.guardados.size)
    }

    @Test
    fun descartarQuitaLaSugerenciaYGuardarConIndiceInvalidoNoHaceNada() = runTest {
        val a = armar(this)
        a.vm.onEvent(OutfitsUiEvent.Sugerir)

        a.vm.onEvent(OutfitsUiEvent.GuardarSugerencia(7))
        assertEquals(1, a.vm.contenido().sugerencias.size)
        assertEquals(3, a.vm.contenido().guardados.size)

        a.vm.onEvent(OutfitsUiEvent.DescartarSugerencia(0))
        assertEquals(emptyList(), a.vm.contenido().sugerencias)
    }

    @Test
    fun elAvisoSeLimpiaCuandoYaSeMostro() = runTest {
        val a = armar(this, sugerencias = Result.success(emptyList()))
        a.vm.onEvent(OutfitsUiEvent.Sugerir)

        a.vm.onEvent(OutfitsUiEvent.AvisoMostrado)

        assertNull(a.vm.contenido().aviso)
    }

    @Test
    fun siNoSePuedenCargarLosOutfitsMuestraElErrorYReintentaVuelveAIntentar() = runTest {
        var falla = true
        val repo = object : OutfitsRepository by FakeOutfitsRepository() {
            override fun observarOutfits(): Flow<List<Outfit>> = flow {
                if (falla) throw DomainException.SinConexion
                emit(emptyList())
            }
        }
        val observePrendas = ObservePrendasUseCase(FakePrendaRepository())
        val vm = OutfitsViewModel(
            ObserveOutfitsUseCase(repo), observePrendas, SaveOutfitUseCase(repo),
            SuggestOutfitsUseCase(observePrendas, SugeridorDePrueba(Result.success(emptyList()))),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        assertEquals(OutfitsUiState.Error(OutfitsTextos.SIN_CONEXION), vm.uiState.value)

        falla = false
        vm.onEvent(OutfitsUiEvent.Reintentar)

        assertIs<OutfitsUiState.Contenido>(vm.uiState.value)
    }

    @Test
    fun enEscritorioLoQueNoEstaDisponibleSeDiceTalCual() {
        val error = DomainException.Desconocido(UnsupportedOperationException("No disponible todavía en escritorio"))

        assertEquals("No disponible todavía en escritorio", error.aMensajeDeOutfits())
    }
}
