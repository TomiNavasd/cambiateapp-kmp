package com.example.cambiateappkmp.presentation.wardrobe

import com.example.cambiateappkmp.data.fake.FakePrendaRepository
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import com.example.cambiateappkmp.domain.usecase.ObservePrendasUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class WardrobeViewModelTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private fun TestScope.armar(repo: PrendaRepository = FakePrendaRepository()): WardrobeViewModel {
        val vm = WardrobeViewModel(ObservePrendasUseCase(repo))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        return vm
    }

    private fun WardrobeViewModel.contenido() = assertIs<WardrobeUiState.Contenido>(uiState.value)

    private fun WardrobeViewModel.nombres() = contenido().prendas.map { it.nombre }

    // Las del fake: 10 prendas (3 superiores, 2 inferiores, 2 calzados, 2 abrigos, 1 accesorio), todas con etiquetas.

    @Test
    fun alAbrirMuestraTodasLasPrendasSinFiltro() = runTest {
        val vm = armar()

        val estado = vm.contenido()

        assertEquals(10, estado.prendas.size)
        assertEquals(null, estado.filtro)
        assertEquals("", estado.consulta)
    }

    @Test
    fun sinPrendasMuestraElEstadoVacio() = runTest {
        val repo = object : PrendaRepository by FakePrendaRepository() {
            override fun observarPrendas(): Flow<List<Prenda>> = flow { emit(emptyList()) }
        }

        assertEquals(WardrobeUiState.Vacio, armar(repo).uiState.value)
    }

    @Test
    fun elChipDeCategoriaFiltraYTodasLoQuita() = runTest {
        val vm = armar()

        vm.onEvent(WardrobeUiEvent.FiltroSeleccionado(Categoria.CALZADO))
        assertEquals(listOf("Zapatillas blancas", "Zapatos de cuero marrón"), vm.nombres())
        assertEquals(Categoria.CALZADO, vm.contenido().filtro)

        vm.onEvent(WardrobeUiEvent.FiltroSeleccionado(null))
        assertEquals(10, vm.contenido().prendas.size)
    }

    @Test
    fun laBusquedaPorNombreNoDistingueTildesNiMayusculas() = runTest {
        val vm = armar()

        vm.onEvent(WardrobeUiEvent.BusquedaCambiada("PANTALON"))

        assertEquals(listOf("Pantalón de vestir negro"), vm.nombres())
        assertEquals("PANTALON", vm.contenido().consulta)
    }

    @Test
    fun laBusquedaTambienMiraColoresCategoriaYEtiquetas() = runTest {
        val vm = armar()

        vm.onEvent(WardrobeUiEvent.BusquedaCambiada("rojo"))
        assertEquals(listOf("Remera negra estampada"), vm.nombres())

        vm.onEvent(WardrobeUiEvent.BusquedaCambiada("accesorio"))
        assertEquals(listOf("Reloj plateado"), vm.nombres())

        vm.onEvent(WardrobeUiEvent.BusquedaCambiada("boda")) // solo está en las etiquetas, que el usuario no ve
        assertEquals(listOf("Zapatos de cuero marrón"), vm.nombres())
    }

    @Test
    fun primeroFiltraPorCategoriaYDespuesPorTexto() = runTest {
        val vm = armar()

        vm.onEvent(WardrobeUiEvent.BusquedaCambiada("azul"))
        assertEquals(listOf("Jean azul", "Campera de jean"), vm.nombres())

        vm.onEvent(WardrobeUiEvent.FiltroSeleccionado(Categoria.ABRIGO))
        assertEquals(listOf("Campera de jean"), vm.nombres())

        vm.onEvent(WardrobeUiEvent.FiltroSeleccionado(Categoria.CALZADO))
        assertEquals(emptyList(), vm.nombres())
    }

    @Test
    fun sinCoincidenciasQuedaElContenidoVacioYNoElEstadoVacio() = runTest {
        val vm = armar()

        vm.onEvent(WardrobeUiEvent.BusquedaCambiada("zzz"))

        assertEquals(emptyList(), vm.nombres())
        assertIs<WardrobeUiState.Contenido>(vm.uiState.value)
    }

    @Test
    fun alAgregarUnaPrendaLaListaSeActualizaRespetandoLosFiltros() = runTest {
        val repo = FakePrendaRepository()
        val vm = armar(repo)
        vm.onEvent(WardrobeUiEvent.FiltroSeleccionado(Categoria.ACCESORIO))

        repo.guardarPrenda(
            Prenda("", "Bufanda", "", Categoria.ACCESORIO, listOf("gris"), emptyList(), "https://x/b.jpg"),
        )

        assertEquals(listOf("Reloj plateado", "Bufanda"), vm.nombres())
    }

    @Test
    fun losErroresSeMuestranConSuMensajeYReintentarVuelveAIntentar() = runTest {
        val errores = listOf(
            DomainException.SinConexion to "Sin conexión. Revisá tu internet e intentá de nuevo",
            DomainException.Desconocido(IllegalStateException("x")) to "No pudimos cargar tu armario",
        )
        for ((error, mensaje) in errores) {
            var falla = true
            val repo = object : PrendaRepository by FakePrendaRepository() {
                override fun observarPrendas(): Flow<List<Prenda>> =
                    if (falla) flow { throw error } else emptyFlow()
            }
            val vm = armar(repo)

            assertEquals(WardrobeUiState.Error(mensaje), vm.uiState.value, "con $error")

            falla = false
            vm.onEvent(WardrobeUiEvent.Reintentar)
            assertEquals(WardrobeUiState.Cargando, vm.uiState.value)
        }
    }
}
