package com.example.cambiateappkmp.presentation.outfits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.domain.model.nombreVisibleDeOutfit
import com.example.cambiateappkmp.domain.model.temporadaTextoDeOutfit
import com.example.cambiateappkmp.presentation.components.BotonCorazon
import com.example.cambiateappkmp.presentation.components.CategoryChip
import com.example.cambiateappkmp.presentation.components.EstadoCargando
import com.example.cambiateappkmp.presentation.components.EstadoError
import com.example.cambiateappkmp.presentation.components.FotoPrenda
import com.example.cambiateappkmp.presentation.wardrobe.etiqueta
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OutfitsRoute(viewModel: OutfitsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    OutfitsScreen(state = state, onEvent = viewModel::onEvent)
}

/** Pantalla "tonta": dibuja el estado y manda eventos hacia arriba. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitsScreen(
    state: OutfitsUiState,
    onEvent: (OutfitsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val aviso = (state as? OutfitsUiState.Contenido)?.aviso
    LaunchedEffect(aviso) {
        if (aviso != null) {
            snackbarHostState.showSnackbar(aviso)
            onEvent(OutfitsUiEvent.AvisoMostrado)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("¿Para qué ocasión?", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                OutfitsUiState.Cargando -> EstadoCargando()
                is OutfitsUiState.Error -> EstadoError(
                    mensaje = state.mensaje,
                    onReintentar = { onEvent(OutfitsUiEvent.Reintentar) },
                )
                is OutfitsUiState.Contenido -> ContenidoOutfits(state = state, onEvent = onEvent)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContenidoOutfits(
    state: OutfitsUiState.Contenido,
    onEvent: (OutfitsUiEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // FlowRow: los chips pasan a la fila de abajo si no entran, así se ven todos.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Ocasion.entries.forEach { ocasion ->
                        CategoryChip(
                            texto = ocasion.etiqueta(),
                            seleccionado = state.ocasion == ocasion,
                            onClick = { onEvent(OutfitsUiEvent.OcasionSeleccionada(ocasion)) },
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) { BannerSugeridos() }

            if (state.sugerencias.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Sugerencias", style = MaterialTheme.typography.titleLarge)
                }
                itemsIndexed(state.sugerencias) { indice, item ->
                    OutfitTarjeta(
                        item = item,
                        favorito = false,
                        descripcionCorazon = "Guardar outfit",
                        onCorazon = { onEvent(OutfitsUiEvent.GuardarSugerencia(indice)) },
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Guardados", style = MaterialTheme.typography.titleLarge)
            }
            if (state.guardados.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Todavía no guardaste outfits. Pedí una sugerencia y guardá la que más te guste.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.guardados, key = { it.outfit.id }) { item ->
                    OutfitTarjeta(item = item)
                }
            }
        }

        BotonGenerar(
            texto = if (state.sugerencias.isEmpty()) "Sugerirme outfits" else "Generar más outfits",
            cargando = state.sugiriendo,
            onClick = { onEvent(OutfitsUiEvent.Sugerir) },
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun BannerSugeridos() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Outfits sugeridos con tu ropa",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Combinaciones creadas con las prendas de tu armario.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun BotonGenerar(
    texto: String,
    cargando: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !cargando,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier.height(52.dp),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Tarjeta de un outfit: las fotos de sus prendas, el nombre, la ocasión y las temporadas. El corazón es opcional. */
@Composable
private fun OutfitTarjeta(
    item: OutfitConPrendas,
    favorito: Boolean = false,
    descripcionCorazon: String = "",
    onCorazon: (() -> Unit)? = null,
) {
    val temporada = item.prendas.temporadaTextoDeOutfit()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item.prendas.take(4).forEach { prenda ->
                        FotoPrenda(
                            url = prenda.fotoUrl,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface),
                        )
                    }
                }
                if (onCorazon != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(36.dp),
                    ) {
                        BotonCorazon(relleno = favorito, descripcion = descripcionCorazon, onClick = onCorazon)
                    }
                }
            }

            Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)) {
                Text(
                    text = item.prendas.nombreVisibleDeOutfit(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(item.outfit.ocasion.etiqueta(), temporada).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
