package com.example.cambiateappkmp.presentation.outfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cambiateappkmp.domain.error.DomainException
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.Outfit
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.domain.usecase.ObserveOutfitsUseCase
import com.example.cambiateappkmp.domain.usecase.ObservePrendasUseCase
import com.example.cambiateappkmp.domain.usecase.SaveOutfitUseCase
import com.example.cambiateappkmp.domain.usecase.SuggestOutfitsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OutfitsViewModel(
    observeOutfits: ObserveOutfitsUseCase,
    observePrendas: ObservePrendasUseCase,
    private val saveOutfit: SaveOutfitUseCase,
    private val suggestOutfits: SuggestOutfitsUseCase,
) : ViewModel() {

    /** Lo que solo vive en esta pantalla: la ocasión elegida, las sugerencias sin guardar y el aviso. */
    private data class Local(
        val ocasion: Ocasion = Ocasion.CASUAL,
        val sugerencias: List<Outfit> = emptyList(),
        val sugiriendo: Boolean = false,
        val aviso: String? = null,
    )

    private val local = MutableStateFlow(Local())
    private val reintento = MutableStateFlow(0)

    val uiState: StateFlow<OutfitsUiState> = reintento
        .flatMapLatest {
            combine(observeOutfits(), observePrendas(), local) { outfits, prendas, estadoLocal ->
                armarEstado(outfits, prendas, estadoLocal)
            }.catch { emit(OutfitsUiState.Error(it.aMensajeDeOutfits())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OutfitsUiState.Cargando)

    fun onEvent(event: OutfitsUiEvent) {
        when (event) {
            is OutfitsUiEvent.OcasionSeleccionada ->
                local.update { it.copy(ocasion = event.ocasion, sugerencias = emptyList()) }
            OutfitsUiEvent.Sugerir -> sugerir()
            is OutfitsUiEvent.GuardarSugerencia -> guardarSugerencia(event.indice)
            is OutfitsUiEvent.DescartarSugerencia -> local.update { actual ->
                actual.copy(sugerencias = actual.sugerencias.filterIndexed { i, _ -> i != event.indice })
            }
            OutfitsUiEvent.Reintentar -> reintento.update { it + 1 }
            OutfitsUiEvent.AvisoMostrado -> local.update { it.copy(aviso = null) }
        }
    }

    private fun armarEstado(outfits: List<Outfit>, prendas: List<Prenda>, estadoLocal: Local): OutfitsUiState {
        val porId = prendas.associateBy { it.id }
        fun Outfit.conPrendas() = OutfitConPrendas(this, prendasIds.mapNotNull { porId[it] })
        return OutfitsUiState.Contenido(
            ocasion = estadoLocal.ocasion,
            sugerencias = estadoLocal.sugerencias.map { it.conPrendas() },
            guardados = outfits.map { it.conPrendas() },
            sugiriendo = estadoLocal.sugiriendo,
            aviso = estadoLocal.aviso,
        )
    }

    private fun sugerir() {
        val ocasion = local.value.ocasion
        viewModelScope.launch {
            local.update { it.copy(sugiriendo = true, aviso = null) }
            suggestOutfits(ocasion).fold(
                onSuccess = { lista ->
                    local.update {
                        it.copy(
                            sugiriendo = false,
                            sugerencias = lista,
                            aviso = if (lista.isEmpty()) OutfitsTextos.SIN_SUGERENCIAS else null,
                        )
                    }
                },
                onFailure = { e -> local.update { it.copy(sugiriendo = false, aviso = e.aMensajeDeOutfits()) } },
            )
        }
    }

    private fun guardarSugerencia(indice: Int) {
        val outfit = local.value.sugerencias.getOrNull(indice) ?: return

        viewModelScope.launch {
            saveOutfit(outfit).fold(
                onSuccess = {
                    local.update { actual ->
                        actual.copy(
                            sugerencias = actual.sugerencias.filterNot { it === outfit },
                            aviso = OutfitsTextos.GUARDADO,
                        )
                    }
                },
                onFailure = { e ->
                    local.update { actual ->
                        // Si ya estaba guardado, la sugerencia sobra: se quita y se avisa.
                        val sugerencias = if (e is DomainException.OutfitYaGuardado) {
                            actual.sugerencias.filterNot { it === outfit }
                        } else {
                            actual.sugerencias
                        }
                        actual.copy(sugerencias = sugerencias, aviso = e.aMensajeDeOutfits())
                    }
                },
            )
        }
    }
}
