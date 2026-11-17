package com.example.cambiateappkmp.presentation.outfits

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    // Estado puramente visual: qué outfit se está por eliminar (para el diálogo).
    var outfitAEliminar by remember { mutableStateOf<String?>(null) }

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
                is OutfitsUiState.Contenido -> ContenidoOutfits(
                    state = state,
                    onEvent = onEvent,
                    onPedirEliminar = { outfitAEliminar = it },
                )
            }
        }
    }

    outfitAEliminar?.let { id ->
        AlertDialog(
            onDismissRequest = { outfitAEliminar = null },
            title = { Text("¿Eliminar outfit?") },
            text = { Text("Se borra el outfit, pero tus prendas se mantienen en el armario.") },
            confirmButton = {
                TextButton(onClick = {
                    outfitAEliminar = null
                    onEvent(OutfitsUiEvent.Eliminar(id))
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { outfitAEliminar = null }) { Text("Cancelar") }
            },
        )
    }
}

/** Qué outfit tiene abierta la hoja de detalles: una sugerencia (por posición) o uno guardado (por id). */
private data class Seleccion(val esSugerencia: Boolean, val clave: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContenidoOutfits(
    state: OutfitsUiState.Contenido,
    onEvent: (OutfitsUiEvent) -> Unit,
    onPedirEliminar: (String) -> Unit,
) {
    var seleccion by remember { mutableStateOf<Seleccion?>(null) }

    val elegido = seleccion?.let { s ->
        if (s.esSugerencia) state.sugerencias.getOrNull(s.clave.toInt())
        else state.guardados.firstOrNull { it.outfit.id == s.clave }
    }

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
                        onVerDetalles = { seleccion = Seleccion(true, indice.toString()) },
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
                    OutfitTarjeta(
                        item = item,
                        favorito = item.outfit.favorito,
                        descripcionCorazon = if (item.outfit.favorito) "Quitar de favoritos" else "Marcar como favorito",
                        onCorazon = { onEvent(OutfitsUiEvent.AlternarFavorito(item.outfit)) },
                        onVerDetalles = { seleccion = Seleccion(false, item.outfit.id) },
                    )
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

    if (seleccion != null && elegido == null) {
        // El outfit ya no existe (se guardó, se descartó o se eliminó): se cierra la hoja.
        LaunchedEffect(seleccion) { seleccion = null }
    }

    if (elegido != null) {
        val esSugerencia = seleccion?.esSugerencia == true
        DetalleOutfitSheet(
            item = elegido,
            esSugerencia = esSugerencia,
            onCerrar = { seleccion = null },
            onGuardar = {
                onEvent(OutfitsUiEvent.GuardarSugerencia(seleccion?.clave?.toInt() ?: 0))
                seleccion = null
            },
            onDescartar = {
                onEvent(OutfitsUiEvent.DescartarSugerencia(seleccion?.clave?.toInt() ?: 0))
                seleccion = null
            },
            onEliminar = {
                onPedirEliminar(elegido.outfit.id)
                seleccion = null
            },
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
    onVerDetalles: (() -> Unit)? = null,
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

            if (onVerDetalles != null) {
                OutlinedButton(
                    onClick = onVerDetalles,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(36.dp),
                ) {
                    Text(
                        text = "Ver detalles",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Hoja con el detalle de un outfit: sus prendas y las acciones (guardar o descartar una sugerencia, eliminar uno guardado). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleOutfitSheet(
    item: OutfitConPrendas,
    esSugerencia: Boolean,
    onCerrar: () -> Unit,
    onGuardar: () -> Unit,
    onDescartar: () -> Unit,
    onEliminar: () -> Unit,
) {
    val temporada = item.prendas.temporadaTextoDeOutfit()

    ModalBottomSheet(onDismissRequest = onCerrar) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = item.prendas.nombreVisibleDeOutfit(), style = MaterialTheme.typography.titleLarge)
            Text(
                text = listOfNotNull(item.outfit.ocasion.etiqueta(), temporada).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            item.prendas.forEach { prenda ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FotoPrenda(
                        url = prenda.fotoUrl,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Column {
                        Text(
                            text = prenda.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = (listOf(prenda.categoria.etiqueta()) + prenda.colores.take(2)).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (esSugerencia) {
                Button(
                    onClick = onGuardar,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Guardar outfit", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onDescartar,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text("Descartar", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                TextButton(onClick = onEliminar, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Eliminar outfit",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
