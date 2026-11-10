package com.example.cambiateappkmp.presentation.wardrobe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.presentation.components.CategoryChip
import com.example.cambiateappkmp.presentation.components.EstadoCargando
import com.example.cambiateappkmp.presentation.components.EstadoError
import com.example.cambiateappkmp.presentation.components.EstadoVacio
import com.example.cambiateappkmp.presentation.components.PrendaCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WardrobeListRoute(
    onPrendaClick: (String) -> Unit,
    onNuevaClick: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: WardrobeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    WardrobeListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onPrendaClick = onPrendaClick,
        onNuevaClick = onNuevaClick,
        onCerrarSesion = onCerrarSesion,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeListScreen(
    state: WardrobeUiState,
    onEvent: (WardrobeUiEvent) -> Unit,
    onPrendaClick: (String) -> Unit,
    onNuevaClick: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Estado puramente visual (diálogo abierto): vive en la pantalla.
    var dialogoAbierto by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mi armario", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    TextButton(onClick = { dialogoAbierto = true }) { Text("Cerrar sesión") }
                },
            )
        },
        floatingActionButton = {
            if (state is WardrobeUiState.Contenido) {
                ExtendedFloatingActionButton(
                    onClick = onNuevaClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text("+ Agregar")
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                WardrobeUiState.Cargando -> EstadoCargando()
                WardrobeUiState.Vacio -> EstadoVacio(
                    titulo = "Tu armario está vacío",
                    mensaje = "Comenzá agregando tus prendas para organizar tu ropa y crear outfits.",
                    textoBoton = "Agregar mi primera prenda",
                    onClick = onNuevaClick,
                )
                is WardrobeUiState.Error -> EstadoError(
                    mensaje = state.mensaje,
                    onReintentar = { onEvent(WardrobeUiEvent.Reintentar) },
                )
                is WardrobeUiState.Contenido -> ContenidoArmario(
                    state = state,
                    onEvent = onEvent,
                    onPrendaClick = onPrendaClick,
                )
            }
        }
    }

    if (dialogoAbierto) {
        AlertDialog(
            onDismissRequest = { dialogoAbierto = false },
            title = { Text("¿Cerrar sesión?") },
            text = {
                Text("Vas a salir de tu cuenta. Tendrás que iniciar sesión nuevamente para acceder a tus prendas.")
            },
            confirmButton = {
                TextButton(onClick = {
                    dialogoAbierto = false
                    onCerrarSesion()
                }) {
                    Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogoAbierto = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun ContenidoArmario(
    state: WardrobeUiState.Contenido,
    onEvent: (WardrobeUiEvent) -> Unit,
    onPrendaClick: (String) -> Unit,
) {
    var texto by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = texto,
            onValueChange = {
                texto = it
                onEvent(WardrobeUiEvent.BusquedaCambiada(it))
            },
            label = { Text("Buscar prenda") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                CategoryChip(
                    texto = "Todas",
                    seleccionado = state.filtro == null,
                    onClick = { onEvent(WardrobeUiEvent.FiltroSeleccionado(null)) },
                )
            }
            items(Categoria.entries) { categoria ->
                CategoryChip(
                    texto = categoria.etiqueta(),
                    seleccionado = state.filtro == categoria,
                    onClick = { onEvent(WardrobeUiEvent.FiltroSeleccionado(categoria)) },
                )
            }
        }

        if (state.prendas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No encontramos prendas con esos filtros",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.prendas, key = { it.id }) { prenda ->
                    PrendaCard(
                        nombre = prenda.nombre,
                        categoria = prenda.categoria.etiqueta(),
                        fotoUrl = prenda.fotoUrl,
                        onClick = { onPrendaClick(prenda.id) },
                    )
                }
            }
        }
    }
}