package com.example.cambiateappkmp.presentation.altaedicion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.error.CampoInvalido
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.AnalisisPrenda
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.usecase.AnalizarPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.EtiquetarPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.ObtenerPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.SavePrendaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** La app está en español: el análisis de la foto se pide en ese idioma. */
private const val IDIOMA = "es"

class AltaEdicionViewModel(
    private val obtenerPrendaUseCase: ObtenerPrendaUseCase,
    private val savePrendaUseCase: SavePrendaUseCase,
    private val analizarPrendaUseCase: AnalizarPrendaUseCase,
    private val etiquetarPrendaUseCase: EtiquetarPrendaUseCase,
) : ViewModel() {

    private var prendaId: String? = null
    private var abierto = false
    private var trabajoCarga: Job? = null
    private var trabajoEscaneo: Job? = null

    /** Si el usuario ya eligió la categoría, el escaneo no se la cambia. */
    private var categoriaTocada = false

    private val _uiState = MutableStateFlow(AltaEdicionUiState())
    val uiState: StateFlow<AltaEdicionUiState> = _uiState.asStateFlow()

    private val _guardada = Channel<Unit>(Channel.BUFFERED)
    val guardada: Flow<Unit> = _guardada.receiveAsFlow()

    fun iniciar(id: String?) {
        if (abierto && prendaId == id) return
        abierto = true
        prendaId = id
        trabajoCarga?.cancel()
        trabajoEscaneo?.cancel()
        categoriaTocada = false
        _uiState.value = AltaEdicionUiState(esEdicion = id != null, cargando = id != null)
        if (id != null) cargarPrenda(id)
    }

    fun cerrar() {
        abierto = false
        prendaId = null
        trabajoCarga?.cancel()
        trabajoEscaneo?.cancel()
        categoriaTocada = false
        _uiState.value = AltaEdicionUiState()
    }

    fun onEvent(event: AltaEdicionUiEvent) {
        when (event) {
            is AltaEdicionUiEvent.NombreCambio ->
                _uiState.update { it.copy(nombre = event.valor, errorNombre = null) }
            is AltaEdicionUiEvent.DescripcionCambio ->
                _uiState.update { it.copy(descripcion = event.valor) }
            is AltaEdicionUiEvent.CategoriaSeleccionada -> {
                categoriaTocada = true
                _uiState.update { it.copy(categoria = event.categoria) }
            }
            is AltaEdicionUiEvent.ColoresCambio ->
                _uiState.update { it.copy(colores = event.valor) }
            is AltaEdicionUiEvent.OcasionAlternada -> _uiState.update {
                val nuevas = if (event.ocasion in it.ocasiones) {
                    it.ocasiones - event.ocasion
                } else {
                    it.ocasiones + event.ocasion
                }
                it.copy(ocasiones = nuevas)
            }
            is AltaEdicionUiEvent.FotoSubida -> _uiState.update {
                // Otra foto es otra prenda: las etiquetas de la anterior ya no sirven y se piden de nuevo al guardar.
                val etiquetas = if (event.url == it.fotoUrl) it.etiquetas else emptyList()
                it.copy(fotoUrl = event.url, errorFoto = null, etiquetas = etiquetas, avisoEscaneo = null)
            }
            AltaEdicionUiEvent.Escanear -> escanear()
            AltaEdicionUiEvent.Guardar -> guardar()
            AltaEdicionUiEvent.ReintentarCarga -> prendaId?.let { cargarPrenda(it) }
        }
    }

    private fun cargarPrenda(id: String) {
        _uiState.update { it.copy(cargando = true, errorCarga = null, noEncontrada = false) }
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            try {
                val prenda = obtenerPrendaUseCase(id)
                if (prenda == null) {
                    _uiState.update { it.copy(cargando = false, noEncontrada = true) }
                } else {
                    categoriaTocada = true
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            nombre = prenda.nombre,
                            descripcion = prenda.descripcion,
                            categoria = prenda.categoria,
                            colores = prenda.colores.joinToString(", "),
                            ocasiones = prenda.ocasiones.toSet(),
                            fotoUrl = prenda.fotoUrl,
                            etiquetas = prenda.etiquetas,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, errorCarga = mensajeDe(e)) }
            }
        }
    }

    private fun guardar() {
        val actual = _uiState.value
        if (actual.guardando || actual.cargando || actual.analizando) return

        if (actual.nombre.isBlank()) {
            _uiState.update { it.copy(errorNombre = "Ingresá un nombre") }
            return
        }

        _uiState.update {
            it.copy(guardando = true, errorNombre = null, errorFoto = null, errorGeneral = null)
        }

        viewModelScope.launch {
            val resultado = try {
                // Si la prenda todavía no tiene etiquetas (no se escaneó), se piden ahora a partir de la foto.
                // Si no se consiguen, se guarda igual, sin etiquetas.
                val etiquetas = actual.etiquetas.ifEmpty { etiquetarPrendaUseCase(actual.fotoUrl) }

                val prenda = Prenda(
                    id = prendaId.orEmpty(),
                    nombre = actual.nombre,
                    descripcion = actual.descripcion.trim(),
                    categoria = actual.categoria,
                    colores = parsearColores(actual.colores),
                    ocasiones = Ocasion.entries.filter { it in actual.ocasiones },
                    fotoUrl = actual.fotoUrl,
                    etiquetas = etiquetas,
                )
                savePrendaUseCase(prenda)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            resultado.fold(
                onSuccess = {
                    _uiState.update { it.copy(guardando = false) }
                    _guardada.trySend(Unit)
                },
                onFailure = { error -> _uiState.update { it.conError(error) } },
            )
        }
    }

    /** Analiza la foto ya subida y completa lo que el usuario dejó vacío; las etiquetas se guardan sin mostrarse. */
    private fun escanear() {
        val actual = _uiState.value
        if (actual.fotoUrl.isBlank() || actual.analizando || actual.guardando || actual.cargando) return

        _uiState.update { it.copy(analizando = true, avisoEscaneo = null, avisoEscaneoEsError = false) }

        trabajoEscaneo?.cancel()
        trabajoEscaneo = viewModelScope.launch {
            analizarPrendaUseCase(actual.fotoUrl, IDIOMA).fold(
                onSuccess = { analisis ->
                    _uiState.update {
                        precargar(it, analisis).copy(
                            analizando = false,
                            etiquetas = analisis.etiquetas,
                            avisoEscaneo = "Completamos los datos con IA. Revisalos y corregí lo que haga falta.",
                            avisoEscaneoEsError = false,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(analizando = false, avisoEscaneo = mensajeDeEscaneo(error), avisoEscaneoEsError = true)
                    }
                },
            )
        }
    }

    /** Solo completa lo que está vacío: lo que el usuario ya escribió o eligió no se pisa. */
    private fun precargar(estado: AltaEdicionUiState, analisis: AnalisisPrenda): AltaEdicionUiState {
        val nombre = if (estado.nombre.isBlank()) analisis.nombre else estado.nombre
        return estado.copy(
            nombre = nombre,
            errorNombre = if (nombre.isNotBlank()) null else estado.errorNombre,
            descripcion = if (estado.descripcion.isBlank()) analisis.descripcion else estado.descripcion,
            colores = if (estado.colores.isBlank()) analisis.colores.joinToString(", ") else estado.colores,
            ocasiones = if (estado.ocasiones.isEmpty()) analisis.ocasiones.toSet() else estado.ocasiones,
            categoria = if (!categoriaTocada && analisis.categoria != null) analisis.categoria else estado.categoria,
        )
    }

    private fun mensajeDeEscaneo(error: Throwable): String = when (error) {
        is DomainException.SinConexion -> "Sin conexión: no pudimos analizar la foto. Completá los datos a mano."
        is DomainException.SinPermisos -> "El análisis con IA no está disponible. Completá los datos a mano."
        else -> "No pudimos analizar la foto. Completá los datos a mano."
    }

    private fun parsearColores(texto: String): List<String> =
        texto.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()

    private fun AltaEdicionUiState.conError(error: Throwable): AltaEdicionUiState {
        val base = copy(guardando = false)
        return when (error) {
            is DomainException.Validacion -> when (error.campo) {
                CampoInvalido.NOMBRE -> base.copy(errorNombre = "Ingresá un nombre")
                CampoInvalido.FOTO -> base.copy(errorFoto = "Elegí una foto para la prenda")
                else -> base.copy(errorGeneral = "Revisá los datos ingresados")
            }
            else -> base.copy(errorGeneral = mensajeDe(error, guardando = true))
        }
    }

    private fun mensajeDe(error: Throwable, guardando: Boolean = false): String = when (error) {
        is DomainException.SinConexion -> "Sin conexión. Revisá tu internet e intentá de nuevo"
        is DomainException.SesionNoIniciada -> "Tu sesión venció. Iniciá sesión de nuevo"
        else -> if (guardando) "No pudimos guardar la prenda" else "No pudimos cargar la prenda"
    }
}