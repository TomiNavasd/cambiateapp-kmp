package com.example.cambiateappkmp.presentation.ajustes

import com.example.cambiateappkmp.data.fake.FakePreferenciasRepository
import com.example.cambiateappkmp.domain.model.TemaApp
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AjustesViewModelTest {

    // Los ViewModel usan el dispatcher Main: en los tests lo reemplazamos por uno de prueba.
    @BeforeTest
    fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun despues() {
        Dispatchers.resetMain()
    }

    @Test
    fun arrancaConElTemaDelSistema() = runTest {
        val viewModel = AjustesViewModel(FakePreferenciasRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect { } }

        assertEquals(TemaApp.SISTEMA, viewModel.estado.value.tema)
    }

    @Test
    fun elegirUnTemaActualizaElEstado() = runTest {
        val viewModel = AjustesViewModel(FakePreferenciasRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect { } }

        viewModel.elegirTema(TemaApp.OSCURO)

        assertEquals(TemaApp.OSCURO, viewModel.estado.value.tema)
    }

    @Test
    fun elegirUnTemaLoGuardaEnLasPreferencias() = runTest {
        val preferencias = FakePreferenciasRepository()
        val viewModel = AjustesViewModel(preferencias)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect { } }

        viewModel.elegirTema(TemaApp.CLARO)

        assertEquals(TemaApp.CLARO, preferencias.tema.first())
    }

    @Test
    fun elEstadoSigueElUltimoTemaElegido() = runTest {
        val viewModel = AjustesViewModel(FakePreferenciasRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.estado.collect { } }

        viewModel.elegirTema(TemaApp.OSCURO)
        viewModel.elegirTema(TemaApp.CLARO)
        viewModel.elegirTema(TemaApp.SISTEMA)

        assertEquals(TemaApp.SISTEMA, viewModel.estado.value.tema)
    }
}
