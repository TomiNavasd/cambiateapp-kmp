package com.example.cambiateappkmp.presentation.altaedicion

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cambiateappkmp.domain.model.Categoria
import com.example.cambiateappkmp.domain.model.Ocasion
import com.example.cambiateappkmp.presentation.components.CategoryChip
import com.example.cambiateappkmp.presentation.components.EstadoCargando
import com.example.cambiateappkmp.presentation.components.EstadoError
import com.example.cambiateappkmp.presentation.components.EstadoVacio
import com.example.cambiateappkmp.presentation.components.SelectorFoto
import com.example.cambiateappkmp.presentation.wardrobe.etiqueta
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AltaEdicionRoute(
    id: String?,
    onVolver: () -> Unit,
    onGuardada: () -> Unit,
    viewModel: AltaEdicionViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(id) { viewModel.iniciar(id) }
    LaunchedEffect(Unit) {
        viewModel.guardada.collect {
            viewModel.cerrar()
            onGuardada()
        }
    }

    AltaEdicionScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onVolver = {
            viewModel.cerrar()
            onVolver()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AltaEdicionScreen(
    state: AltaEdicionUiState,
    onEvent: (AltaEdicionUiEvent) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.esEdicion) "Editar prenda" else "Nueva prenda",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.cargando -> EstadoCargando()
                state.noEncontrada -> EstadoVacio(
                    titulo = "No encontramos esta prenda",
                    mensaje = "Puede que ya no exista.",
                    textoBoton = "Volver",
                    onClick = onVolver,
                )
                state.errorCarga != null -> EstadoError(
                    mensaje = state.errorCarga,
                    onReintentar = { onEvent(AltaEdicionUiEvent.ReintentarCarga) },
                )
                else -> Formulario(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun Formulario(
    state: AltaEdicionUiState,
    onEvent: (AltaEdicionUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // La foto la sube el selector y devuelve la URL ya lista.
        SelectorFoto(
            fotoUrl = state.fotoUrl,
            onFotoSubida = { url -> onEvent(AltaEdicionUiEvent.FotoSubida(url)) },
        )
        if (state.errorFoto != null) {
            Text(
                text = state.errorFoto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        OutlinedTextField(
            value = state.nombre,
            onValueChange = { onEvent(AltaEdicionUiEvent.NombreCambio(it)) },
            label = { Text("Nombre") },
            singleLine = true,
            isError = state.errorNombre != null,
            supportingText = state.errorNombre?.let { mensaje -> { Text(mensaje) } },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.descripcion,
            onValueChange = { onEvent(AltaEdicionUiEvent.DescripcionCambio(it)) },
            label = { Text("Descripción (opcional)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Categoría", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Categoria.entries.forEach { categoria ->
                CategoryChip(
                    texto = categoria.etiqueta(),
                    seleccionado = state.categoria == categoria,
                    onClick = { onEvent(AltaEdicionUiEvent.CategoriaSeleccionada(categoria)) },
                )
            }
        }

        OutlinedTextField(
            value = state.colores,
            onValueChange = { onEvent(AltaEdicionUiEvent.ColoresCambio(it)) },
            label = { Text("Colores (separados por coma)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Ocasiones", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Ocasion.entries.forEach { ocasion ->
                CategoryChip(
                    texto = ocasion.etiqueta(),
                    seleccionado = ocasion in state.ocasiones,
                    onClick = { onEvent(AltaEdicionUiEvent.OcasionAlternada(ocasion)) },
                )
            }
        }

        if (state.errorGeneral != null) {
            Text(
                text = state.errorGeneral,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = { onEvent(AltaEdicionUiEvent.Guardar) },
            enabled = !state.guardando,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(
                text = if (state.guardando) "Guardando..." else "Guardar",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}