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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private class AnalizadorControlable(var resultado: Result<AnalisisPrenda>) : AnalizadorDePrenda {
    var llamadas = 0
    var ultimaFoto: String? = null
    var compuerta: CompletableDeferred<Unit>? = null
    override suspend fun analizar(fotoUrl: String, idioma: String): Result<AnalisisPrenda> {
        llamadas++
        ultimaFoto = fotoUrl
        compuerta?.await()
        return resultado
    }
}

private class RepoQueRegistra(private val existente: Prenda? = null) : PrendaRepository {
    val guardadas = mutableListOf<Prenda>()
    override fun observarPrendas(): Flow<List<Prenda>> = emptyFlow()
    override suspend fun obtenerPrenda(id: String): Prenda? = existente
    override suspend fun guardarPrenda(prenda: Prenda): Result<Unit> {
        guardadas += prenda
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AltaEdicionEscaneoTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private val foto = "https://res.cloudinary.com/demo/prenda.jpg"

    private val analisis = AnalisisPrenda(
        nombre = "Jean azul recto",
        descripcion = "Jean de corte recto",
        categoria = Categoria.INFERIOR,
        colores = listOf("azul"),
        ocasiones = listOf(Ocasion.CASUAL, Ocasion.FACULTAD),
        etiquetas = listOf("casual", "jean", "recto", "diario", "versatil"),
    )

    private class Armado(val vm: AltaEdicionViewModel, val analizador: AnalizadorControlable, val repo: RepoQueRegistra)

    private fun armar(
        resultado: Result<AnalisisPrenda> = Result.success(analisis),
        existente: Prenda? = null,
    ): Armado {
        val analizador = AnalizadorControlable(resultado)
        val repo = RepoQueRegistra(existente)
        val analizar = AnalizarPrendaUseCase(analizador)
        val vm = AltaEdicionViewModel(ObtenerPrendaUseCase(repo), SavePrendaUseCase(repo), analizar, EtiquetarPrendaUseCase(analizar))
        return Armado(vm, analizador, repo)
    }

    private fun Armado.nuevaConFoto(): Armado {
        vm.iniciar(null)
        vm.onEvent(AltaEdicionUiEvent.FotoSubida(foto))
        return this
    }

    @Test
    fun sinFotoNoSePuedeEscanear() = runTest {
        val a = armar()
        a.vm.iniciar(null)

        a.vm.onEvent(AltaEdicionUiEvent.Escanear)

        assertEquals(0, a.analizador.llamadas)
        assertFalse(a.vm.uiState.value.analizando)
    }

    @Test
    fun escanearCompletaNombreDescripcionCategoriaColoresYOcasiones() = runTest {
        val a = armar().nuevaConFoto()

        a.vm.onEvent(AltaEdicionUiEvent.Escanear)

        val s = a.vm.uiState.value
        assertEquals(foto, a.analizador.ultimaFoto)
        assertEquals("Jean azul recto", s.nombre)
        assertEquals("Jean de corte recto", s.descripcion)
        assertEquals(Categoria.INFERIOR, s.categoria)
        assertEquals("azul", s.colores)
        assertEquals(setOf(Ocasion.CASUAL, Ocasion.FACULTAD), s.ocasiones)
        assertEquals(listOf("casual", "jean", "recto", "diario", "versatil"), s.etiquetas)
        assertFalse(s.analizando)
        assertFalse(s.avisoEscaneoEsError)
        assertTrue(s.avisoEscaneo!!.startsWith("Completamos los datos con IA"))
    }

    @Test
    fun noPisaLoQueElUsuarioYaEscribioNiLaCategoriaQueEligio() = runTest {
        val a = armar().nuevaConFoto()
        a.vm.onEvent(AltaEdicionUiEvent.NombreCambio("Mi jean favorito"))
        a.vm.onEvent(AltaEdicionUiEvent.DescripcionCambio("Comprado en Once"))
        a.vm.onEvent(AltaEdicionUiEvent.ColoresCambio("celeste"))
        a.vm.onEvent(AltaEdicionUiEvent.OcasionAlternada(Ocasion.SALIDA))
        a.vm.onEvent(AltaEdicionUiEvent.CategoriaSeleccionada(Categoria.ABRIGO))

        a.vm.onEvent(AltaEdicionUiEvent.Escanear)

        val s = a.vm.uiState.value
        assertEquals("Mi jean favorito", s.nombre)
        assertEquals("Comprado en Once", s.descripcion)
        assertEquals("celeste", s.colores)
        assertEquals(setOf(Ocasion.SALIDA), s.ocasiones)
        assertEquals(Categoria.ABRIGO, s.categoria)
        assertEquals(analisis.etiquetas, s.etiquetas, "las etiquetas sí se guardan")
    }

    @Test
    fun mientrasAnalizaEstaOcupadoYNoSeGuardaNiSeEscaneaDeNuevo() = runTest {
        val a = armar().nuevaConFoto()
        val compuerta = CompletableDeferred<Unit>()
        a.analizador.compuerta = compuerta
        a.vm.onEvent(AltaEdicionUiEvent.NombreCambio("Jean"))

        a.vm.onEvent(AltaEdicionUiEvent.Escanear)
        assertTrue(a.vm.uiState.value.analizando)

        a.vm.onEvent(AltaEdicionUiEvent.Escanear)
        a.vm.onEvent(AltaEdicionUiEvent.Guardar)
        assertEquals(1, a.analizador.llamadas)
        assertTrue(a.repo.guardadas.isEmpty())

        compuerta.complete(Unit)
        assertFalse(a.vm.uiState.value.analizando)
    }

    @Test
    fun losErroresDelEscaneoSeMuestranYSeDejaCompletarAMano() = runTest {
        val casos = listOf(
            DomainException.SinConexion to "Sin conexión: no pudimos analizar la foto. Completá los datos a mano.",
            DomainException.SinPermisos to "El análisis con IA no está disponible. Completá los datos a mano.",
            DomainException.Desconocido(IllegalStateException("x")) to "No pudimos analizar la foto. Completá los datos a mano.",
        )
        for ((error, mensaje) in casos) {
            val a = armar(resultado = Result.failure(error)).nuevaConFoto()

            a.vm.onEvent(AltaEdicionUiEvent.Escanear)

            val s = a.vm.uiState.value
            assertEquals(mensaje, s.avisoEscaneo, "con $error")
            assertTrue(s.avisoEscaneoEsError)
            assertFalse(s.analizando)
            assertEquals("", s.nombre)
            assertEquals(emptyList(), s.etiquetas)
        }
    }

    @Test
    fun alGuardarUnaPrendaEscaneadaSeUsanSusEtiquetasSinPedirlasOtraVez() = runTest {
        val a = armar().nuevaConFoto()
        a.vm.onEvent(AltaEdicionUiEvent.Escanear)

        a.vm.onEvent(AltaEdicionUiEvent.Guardar)

        assertEquals(1, a.analizador.llamadas, "solo el escaneo, no un segundo pedido al guardar")
        assertEquals(analisis.etiquetas, a.repo.guardadas.single().etiquetas)
    }

    @Test
    fun siAlGuardarNoHayEtiquetasSePidenDeNuevoYSeGuardan() = runTest {
        val a = armar().nuevaConFoto()
        a.vm.onEvent(AltaEdicionUiEvent.NombreCambio("Jean"))

        a.vm.onEvent(AltaEdicionUiEvent.Guardar)

        assertEquals(1, a.analizador.llamadas)
        assertEquals(foto, a.analizador.ultimaFoto)
        assertEquals(analisis.etiquetas, a.repo.guardadas.single().etiquetas)
        assertEquals("Jean", a.repo.guardadas.single().nombre)
    }

    @Test
    fun siNoSeConsiguenEtiquetasLaPrendaSeGuardaIgualSinEllas() = runTest {
        val a = armar(resultado = Result.failure(DomainException.SinConexion)).nuevaConFoto()
        a.vm.onEvent(AltaEdicionUiEvent.NombreCambio("Jean"))

        a.vm.onEvent(AltaEdicionUiEvent.Guardar)

        assertEquals(emptyList(), a.repo.guardadas.single().etiquetas)
    }

    @Test
    fun unaPrendaEditadaQueYaTieneEtiquetasNoLasPideOtraVez() = runTest {
        val existente = Prenda("p1", "Remera", "", Categoria.SUPERIOR, listOf("blanco"), listOf(Ocasion.CASUAL), foto, listOf("liviano", "verano"))
        val a = armar(existente = existente)
        a.vm.iniciar("p1")

        a.vm.onEvent(AltaEdicionUiEvent.Guardar)

        assertEquals(0, a.analizador.llamadas)
        assertEquals(listOf("liviano", "verano"), a.repo.guardadas.single().etiquetas)
    }

    @Test
    fun alEditarElEscaneoNoCambiaLaCategoriaQueYaTenia() = runTest {
        val existente = Prenda("p1", "Remera", "", Categoria.SUPERIOR, listOf("blanco"), listOf(Ocasion.CASUAL), foto)
        val a = armar(existente = existente)
        a.vm.iniciar("p1")

        a.vm.onEvent(AltaEdicionUiEvent.Escanear)

        assertEquals(Categoria.SUPERIOR, a.vm.uiState.value.categoria)
        assertEquals("Remera", a.vm.uiState.value.nombre)
    }

    @Test
    fun otraFotoDescartaLasEtiquetasDeLaAnteriorYLaMismaFotoLasConserva() = runTest {
        val a = armar().nuevaConFoto()
        a.vm.onEvent(AltaEdicionUiEvent.Escanear)
        assertEquals(5, a.vm.uiState.value.etiquetas.size)

        a.vm.onEvent(AltaEdicionUiEvent.FotoSubida(foto))
        assertEquals(5, a.vm.uiState.value.etiquetas.size)

        a.vm.onEvent(AltaEdicionUiEvent.FotoSubida("https://res.cloudinary.com/demo/otra.jpg"))
        assertEquals(emptyList(), a.vm.uiState.value.etiquetas)
        assertNull(a.vm.uiState.value.avisoEscaneo)
    }

    @Test
    fun alCerrarElFormularioSeCancelaElEscaneoEnCurso() = runTest {
        val a = armar().nuevaConFoto()
        a.analizador.compuerta = CompletableDeferred()
        a.vm.onEvent(AltaEdicionUiEvent.Escanear)

        a.vm.cerrar()
        a.analizador.compuerta!!.complete(Unit)

        assertFalse(a.vm.uiState.value.analizando)
        assertEquals("", a.vm.uiState.value.nombre)
    }
}
