package com.example.cambiateappkmp.presentation.detalle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cambiateappkmp.domain.model.Prenda
import com.example.cambiateappkmp.presentation.components.EstadoCargando
import com.example.cambiateappkmp.presentation.components.EstadoError
import com.example.cambiateappkmp.presentation.components.EstadoVacio
import com.example.cambiateappkmp.presentation.components.FotoPrenda
import com.example.cambiateappkmp.presentation.wardrobe.etiqueta
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DetallePrendaRoute(
    id: String,
    onVolver: () -> Unit,
    onEditarClick: () -> Unit,
    viewModel: DetallePrendaViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(id) { viewModel.cargar(id) }

    val actual = estado
    val visible = if (actual is DetallePrendaUiState.Contenido && actual.prenda.id != id) {
        DetallePrendaUiState.Cargando
    } else {
        actual
    }

    DetallePrendaScreen(
        state = visible,
        onVolver = onVolver,
        onEditarClick = onEditarClick,
        onReintentar = viewModel::reintentar,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetallePrendaScreen(
    state: DetallePrendaUiState,
    onVolver: () -> Unit,
    onEditarClick: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Detalle", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } },
                actions = {
                    if (state is DetallePrendaUiState.Contenido) {
                        TextButton(onClick = onEditarClick) { Text("Editar") }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                DetallePrendaUiState.Cargando -> EstadoCargando()
                DetallePrendaUiState.NoEncontrada -> EstadoVacio(
                    titulo = "No encontramos esta prenda",
                    mensaje = "Puede que ya no exista.",
                    textoBoton = "Volver",
                    onClick = onVolver,
                )
                is DetallePrendaUiState.Error -> EstadoError(
                    mensaje = state.mensaje,
                    onReintentar = onReintentar,
                )
                is DetallePrendaUiState.Contenido -> ContenidoDetalle(state.prenda)
            }
        }
    }
}

@Composable
private fun ContenidoDetalle(prenda: Prenda) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FotoPrenda(
            url = prenda.fotoUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(16.dp)),
        )

        Text(text = prenda.nombre, style = MaterialTheme.typography.headlineSmall)

        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                text = prenda.categoria.etiqueta(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        if (prenda.descripcion.isNotBlank()) {
            Text(text = prenda.descripcion, style = MaterialTheme.typography.bodyLarge)
        }

        DatoDetalle(
            titulo = "Colores",
            valor = prenda.colores
                .joinToString(", ") { color -> color.replaceFirstChar { it.uppercase() } }
                .ifEmpty { "Sin especificar" },
        )
        DatoDetalle(
            titulo = "Ocasiones",
            valor = prenda.ocasiones
                .joinToString(", ") { it.etiqueta() }
                .ifEmpty { "Sin especificar" },
        )
    }
}

@Composable
private fun DatoDetalle(titulo: String, valor: String) {
    Column {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = valor, style = MaterialTheme.typography.bodyLarge)
    }
}