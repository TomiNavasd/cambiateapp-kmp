package com.example.cambiateappkmp.presentation.notificaciones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cambiateappkmp.presentation.permisos.rememberSolicitudNotificaciones
import com.example.cambiateappkmp.presentation.permisos.rememberSolicitudUbicacion
import org.koin.compose.viewmodel.koinViewModel

/** Qué estaba haciendo el usuario cuando pedimos el permiso de notificaciones. */
private enum class AccionPendiente { ACTIVAR, PROBAR }

@Composable
fun NotificacionesRoute(
    onVolver: () -> Unit,
    viewModel: NotificacionesViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var pendiente by remember { mutableStateOf<AccionPendiente?>(null) }

    val solicitudUbicacion = rememberSolicitudUbicacion { concedido ->
        if (concedido) viewModel.actualizarUbicacion() else viewModel.ubicacionRechazada()
    }

    // Al activar el recordatorio también conviene tener la ubicación, para saber el clima de la zona.
    fun activarConUbicacion() {
        viewModel.activar()
        if (solicitudUbicacion.concedido) viewModel.actualizarUbicacion() else solicitudUbicacion.pedir()
    }

    val solicitudNotificaciones = rememberSolicitudNotificaciones { concedido ->
        val accion = pendiente
        pendiente = null
        if (!concedido) {
            viewModel.permisoNotificacionesRechazado()
        } else {
            when (accion) {
                AccionPendiente.ACTIVAR -> activarConUbicacion()
                AccionPendiente.PROBAR -> viewModel.enviarPrueba()
                null -> Unit
            }
        }
    }

    NotificacionesScreen(
        state = estado,
        onActivadasChange = { activar ->
            if (!activar) {
                viewModel.desactivar()
            } else if (solicitudNotificaciones.concedido) {
                activarConUbicacion()
            } else {
                pendiente = AccionPendiente.ACTIVAR
                solicitudNotificaciones.pedir()
            }
        },
        onHoraElegida = viewModel::elegirHora,
        onProbarClick = {
            if (solicitudNotificaciones.concedido) {
                viewModel.enviarPrueba()
            } else {
                pendiente = AccionPendiente.PROBAR
                solicitudNotificaciones.pedir()
            }
        },
        onAvisoMostrado = viewModel::avisoMostrado,
        onVolver = onVolver,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    state: NotificacionesUiState,
    onActivadasChange: (Boolean) -> Unit,
    onHoraElegida: (Int, Int) -> Unit,
    onProbarClick: () -> Unit,
    onAvisoMostrado: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var mostrarDialogoHora by remember { mutableStateOf(false) }

    LaunchedEffect(state.aviso) {
        state.aviso?.let {
            snackbarHostState.showSnackbar(it)
            onAvisoMostrado()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { TextButton(onClick = onVolver) { Text("Volver") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Recordatorios del clima", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.activadas, onCheckedChange = onActivadasChange)
            }

            Text(
                text = "Te avisamos todos los días qué ponerte según el pronóstico de mañana.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Hora del aviso", style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { mostrarDialogoHora = true }) {
                    Text(text = formatearHora(state.hora, state.minuto), style = MaterialTheme.typography.titleMedium)
                }
            }

            OutlinedButton(
                onClick = onProbarClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(text = "Enviar notificación de prueba", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (mostrarDialogoHora) {
        DialogoHora(
            horaInicial = state.hora,
            minutoInicial = state.minuto,
            onConfirmar = { hora, minuto ->
                mostrarDialogoHora = false
                onHoraElegida(hora, minuto)
            },
            onCancelar = { mostrarDialogoHora = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoHora(
    horaInicial: Int,
    minutoInicial: Int,
    onConfirmar: (Int, Int) -> Unit,
    onCancelar: () -> Unit,
) {
    val estadoReloj = rememberTimePickerState(
        initialHour = horaInicial,
        initialMinute = minutoInicial,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Hora del recordatorio") },
        text = { TimeInput(state = estadoReloj) },
        confirmButton = {
            TextButton(onClick = { onConfirmar(estadoReloj.hour, estadoReloj.minute) }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}
