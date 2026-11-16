package com.example.cambiateappkmp.presentation.ajustes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cambiateappkmp.domain.model.TemaApp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AjustesRoute(
    onVolver: () -> Unit,
    onNotificacionesClick: () -> Unit,
    viewModel: AjustesViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    AjustesScreen(
        state = estado,
        onTemaElegido = viewModel::elegirTema,
        onNotificacionesClick = onNotificacionesClick,
        onVolver = onVolver,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    state: AjustesUiState,
    onTemaElegido: (TemaApp) -> Unit,
    onNotificacionesClick: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Apariencia", style = MaterialTheme.typography.titleMedium)

            Column(modifier = Modifier.selectableGroup()) {
                TemaApp.entries.forEach { tema ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = state.tema == tema,
                                onClick = { onTemaElegido(tema) },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = state.tema == tema, onClick = null)
                        Text(
                            text = tema.etiqueta(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(text = "Notificaciones", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNotificacionesClick)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Recordatorios del clima", style = MaterialTheme.typography.bodyLarge)
                Text(text = "›", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun TemaApp.etiqueta(): String = when (this) {
    TemaApp.SISTEMA -> "Automático (según el dispositivo)"
    TemaApp.CLARO -> "Claro"
    TemaApp.OSCURO -> "Oscuro"
}
