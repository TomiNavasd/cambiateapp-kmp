package com.example.cambiateappkmp.presentation.altaedicion

import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import com.example.cambiateappkmp.domain.usecase.ObtenerPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.SavePrendaUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private class RepositorioQueGuarda(private val existente: Prenda?) : PrendaRepository {
    val guardadas = mutableListOf<Prenda>()
    override fun observarPrendas(): Flow<List<Prenda>> = emptyFlow()
    override suspend fun obtenerPrenda(id: String): Prenda? = existente
    override suspend fun guardarPrenda(prenda: Prenda): Result<Unit> {
        guardadas += prenda
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AltaEdicionEtiquetasTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private val deLaNativa = Prenda(
        id = "p1",
        nombre = "Remera",
        descripcion = "Lisa",
        categoria = Categoria.SUPERIOR,
        colores = listOf("blanco"),
        ocasiones = listOf(Ocasion.CASUAL),
        fotoUrl = "https://x/y.jpg",
        etiquetas = listOf("casual", "liviano", "verano"),
    )

    private fun viewModel(repo: RepositorioQueGuarda) =
        AltaEdicionViewModel(ObtenerPrendaUseCase(repo), SavePrendaUseCase(repo))

    @Test
    fun alEditarSeVuelvenAGuardarLasEtiquetasQueTraiaLaPrenda() = runTest {
        val repo = RepositorioQueGuarda(deLaNativa)
        val vm = viewModel(repo)
        vm.iniciar("p1")

        vm.onEvent(AltaEdicionUiEvent.NombreCambio("Remera blanca"))
        vm.onEvent(AltaEdicionUiEvent.Guardar)

        val guardada = repo.guardadas.single()
        assertEquals("Remera blanca", guardada.nombre)
        assertEquals(listOf("casual", "liviano", "verano"), guardada.etiquetas)
    }

    @Test
    fun unaPrendaNuevaSeGuardaSinEtiquetas() = runTest {
        val repo = RepositorioQueGuarda(null)
        val vm = viewModel(repo)
        vm.iniciar(null)

        vm.onEvent(AltaEdicionUiEvent.NombreCambio("Campera"))
        vm.onEvent(AltaEdicionUiEvent.FotoSubida("https://x/c.jpg"))
        vm.onEvent(AltaEdicionUiEvent.Guardar)

        assertEquals(emptyList(), repo.guardadas.single().etiquetas)
    }
}
