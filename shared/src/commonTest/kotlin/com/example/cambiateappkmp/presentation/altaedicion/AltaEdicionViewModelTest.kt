package com.example.cambiateappkmp.presentation.altaedicion

import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.AnalisisPrenda
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.repository.AnalizadorDePrenda
import com.example.cambiateappkmp.domain.repository.PrendaRepository
import com.example.cambiateappkmp.domain.usecase.AnalizarPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.EtiquetarPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.ObtenerPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.SavePrendaUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private object SinAnalisis : AnalizadorDePrenda {
    override suspend fun analizar(fotoUrl: String, idioma: String): Result<AnalisisPrenda> =
        Result.failure(DomainException.SinPermisos)
}

private class RepoDeFormulario(var existente: Prenda? = null) : PrendaRepository {
    var errorAlObtener: Throwable? = null
    var errorAlGuardar: Throwable? = null
    val guardadas = mutableListOf<Prenda>()
    override fun observarPrendas(): Flow<List<Prenda>> = emptyFlow()
    override suspend fun obtenerPrenda(id: String): Prenda? = errorAlObtener?.let { throw it } ?: existente
    override suspend fun guardarPrenda(prenda: Prenda): Result<Unit> {
        errorAlGuardar?.let { return Result.failure(it) }
        guardadas += prenda
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AltaEdicionViewModelTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private val foto = "https://res.cloudinary.com/demo/prenda.jpg"

    private val existente = Prenda(
        id = "p1",
        nombre = "Remera blanca",
        descripcion = "Lisa",
        categoria = Categoria.SUPERIOR,
        colores = listOf("blanco", "gris"),
        ocasiones = listOf(Ocasion.CASUAL, Ocasion.DEPORTE),
        fotoUrl = foto,
        etiquetas = listOf("casual", "liviano"),
    )

    private fun armar(repo: RepoDeFormulario): AltaEdicionViewModel {
        val analizar = AnalizarPrendaUseCase(SinAnalisis)
        return AltaEdicionViewModel(ObtenerPrendaUseCase(repo), SavePrendaUseCase(repo), analizar, EtiquetarPrendaUseCase(analizar))
    }

    private fun TestScope.contarGuardadas(vm: AltaEdicionViewModel): IntArray {
        val contador = IntArray(1)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.guardada.collect { contador[0]++ } }
        return contador
    }

    @Test
    fun unaPrendaNuevaArrancaVaciaYEnModoAlta() = runTest {
        val vm = armar(RepoDeFormulario())

        vm.iniciar(null)

        val s = vm.uiState.value
        assertFalse(s.esEdicion)
        assertFalse(s.cargando)
        assertEquals("", s.nombre)
        assertEquals(Categoria.SUPERIOR, s.categoria)
        assertEquals(emptySet(), s.ocasiones)
    }

    @Test
    fun sinNombreNoSeGuardaYSeMarcaElCampo() = runTest {
        val repo = RepoDeFormulario()
        val vm = armar(repo)
        vm.iniciar(null)
        vm.onEvent(AltaEdicionUiEvent.NombreCambio("   "))

        vm.onEvent(AltaEdicionUiEvent.Guardar)

        assertEquals("Ingresá un nombre", vm.uiState.value.errorNombre)
        assertTrue(repo.guardadas.isEmpty())
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun alEscribirElNombreSeLimpiaElError() = runTest {
        val vm = armar(RepoDeFormulario())
        vm.iniciar(null)
        vm.onEvent(AltaEdicionUiEvent.Guardar)

        vm.onEvent(AltaEdicionUiEvent.NombreCambio("Jean"))

        assertNull(vm.uiState.value.errorNombre)
    }

    @Test
    fun sinFotoNoSeGuardaYPideElegirUna() = runTest {
        val repo = RepoDeFormulario()
        val vm = armar(repo)
        vm.iniciar(null)
        vm.onEvent(AltaEdicionUiEvent.NombreCambio("Jean"))

        vm.onEvent(AltaEdicionUiEvent.Guardar)

        assertEquals("Elegí una foto para la prenda", vm.uiState.value.errorFoto)
        assertTrue(repo.guardadas.isEmpty())
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun guardarUnaPrendaNuevaArmaLosDatosYAvisaQueSeGuardo() = runTest {
        val repo = RepoDeFormulario()
        val vm = armar(repo)
        val guardadas = contarGuardadas(vm)
        vm.iniciar(null)
        vm.onEvent(AltaEdicionUiEvent.NombreCambio("  Jean azul "))
        vm.onEvent(AltaEdicionUiEvent.DescripcionCambio("  Recto  "))
        vm.onEvent(AltaEdicionUiEvent.CategoriaSeleccionada(Categoria.INFERIOR))
        vm.onEvent(AltaEdicionUiEvent.ColoresCambio("Azul, NEGRO, azul, , "))
        vm.onEvent(AltaEdicionUiEvent.OcasionAlternada(Ocasion.FACULTAD))
        vm.onEvent(AltaEdicionUiEvent.OcasionAlternada(Ocasion.CASUAL))
        vm.onEvent(AltaEdicionUiEvent.FotoSubida(foto))

        vm.onEvent(AltaEdicionUiEvent.Guardar)

        val prenda = repo.guardadas.single()
        assertEquals("", prenda.id)
        assertEquals("Jean azul", prenda.nombre)
        assertEquals("Recto", prenda.descripcion)
        assertEquals(Categoria.INFERIOR, prenda.categoria)
        assertEquals(listOf("azul", "negro"), prenda.colores) // minúsculas, sin vacíos ni repetidos
        assertEquals(listOf(Ocasion.FACULTAD, Ocasion.CASUAL).sortedBy { it.ordinal }, prenda.ocasiones)
        assertEquals(foto, prenda.fotoUrl)
        assertEquals(1, guardadas[0])
        assertFalse(vm.uiState.value.guardando)
    }

    @Test
    fun tocarUnaOcasionDosVecesLaSaca() = runTest {
        val vm = armar(RepoDeFormulario())
        vm.iniciar(null)

        vm.onEvent(AltaEdicionUiEvent.OcasionAlternada(Ocasion.SALIDA))
        assertEquals(setOf(Ocasion.SALIDA), vm.uiState.value.ocasiones)

        vm.onEvent(AltaEdicionUiEvent.OcasionAlternada(Ocasion.SALIDA))
        assertEquals(emptySet(), vm.uiState.value.ocasiones)
    }

    @Test
    fun editarCargaLosDatosDeLaPrendaEnElFormulario() = runTest {
        val vm = armar(RepoDeFormulario(existente))

        vm.iniciar("p1")

        val s = vm.uiState.value
        assertTrue(s.esEdicion)
        assertFalse(s.cargando)
        assertEquals("Remera blanca", s.nombre)
        assertEquals("Lisa", s.descripcion)
        assertEquals(Categoria.SUPERIOR, s.categoria)
        assertEquals("blanco, gris", s.colores)
        assertEquals(setOf(Ocasion.CASUAL, Ocasion.DEPORTE), s.ocasiones)
        assertEquals(foto, s.fotoUrl)
    }

    @Test
    fun editarGuardaConElIdDeLaPrendaYAvisa() = runTest {
        val repo = RepoDeFormulario(existente)
        val vm = armar(repo)
        val guardadas = contarGuardadas(vm)
        vm.iniciar("p1")
        vm.onEvent(AltaEdicionUiEvent.NombreCambio("Remera blanca básica"))

        vm.onEvent(AltaEdicionUiEvent.Guardar)

        val prenda = repo.guardadas.single()
        assertEquals("p1", prenda.id)
        assertEquals("Remera blanca básica", prenda.nombre)
        assertEquals(listOf("blanco", "gris"), prenda.colores)
        assertEquals(1, guardadas[0])
    }

    @Test
    fun siLaPrendaNoExisteSeMuestraQueNoSeEncontro() = runTest {
        val vm = armar(RepoDeFormulario(existente = null))

        vm.iniciar("fantasma")

        assertTrue(vm.uiState.value.noEncontrada)
        assertFalse(vm.uiState.value.cargando)
    }

    @Test
    fun siFallaLaCargaSeMuestraElErrorYReintentarLaCarga() = runTest {
        val repo = RepoDeFormulario(existente)
        repo.errorAlObtener = DomainException.SinConexion
        val vm = armar(repo)

        vm.iniciar("p1")
        assertEquals("Sin conexión. Revisá tu internet e intentá de nuevo", vm.uiState.value.errorCarga)

        repo.errorAlObtener = null
        vm.onEvent(AltaEdicionUiEvent.ReintentarCarga)

        assertNull(vm.uiState.value.errorCarga)
        assertEquals("Remera blanca", vm.uiState.value.nombre)
    }

    @Test
    fun losErroresAlGuardarSeMuestranYSeSigueEditando() = runTest {
        val casos = listOf(
            DomainException.SinConexion to "Sin conexión. Revisá tu internet e intentá de nuevo",
            DomainException.SesionNoIniciada to "Tu sesión venció. Iniciá sesión de nuevo",
            DomainException.Desconocido(IllegalStateException("x")) to "No pudimos guardar la prenda",
        )
        for ((error, mensaje) in casos) {
            val repo = RepoDeFormulario(existente)
            repo.errorAlGuardar = error
            val vm = armar(repo)
            val guardadas = contarGuardadas(vm)
            vm.iniciar("p1")

            vm.onEvent(AltaEdicionUiEvent.Guardar)

            assertEquals(mensaje, vm.uiState.value.errorGeneral, "con $error")
            assertFalse(vm.uiState.value.guardando)
            assertEquals(0, guardadas[0])
            assertEquals("Remera blanca", vm.uiState.value.nombre, "lo escrito no se pierde")
        }
    }

    @Test
    fun cerrarVuelveElFormularioAEmpezarDeCero() = runTest {
        val vm = armar(RepoDeFormulario(existente))
        vm.iniciar("p1")

        vm.cerrar()

        assertEquals(AltaEdicionUiState(), vm.uiState.value)
    }

    @Test
    fun abrirOtraVezLaMismaPrendaNoLaRecarga() = runTest {
        val repo = RepoDeFormulario(existente)
        val vm = armar(repo)
        vm.iniciar("p1")
        vm.onEvent(AltaEdicionUiEvent.NombreCambio("Editado a medias"))

        vm.iniciar("p1")

        assertEquals("Editado a medias", vm.uiState.value.nombre)
    }
}
