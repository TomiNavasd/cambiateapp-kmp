package com.example.cambiateappkmp.presentation.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.LocalRopa
import com.example.cambiateappkmp.presentation.components.ClimaCard
import com.example.cambiateappkmp.presentation.components.EstadoCargando
import com.example.cambiateappkmp.presentation.components.EstadoError
import com.example.cambiateappkmp.presentation.components.EstadoVacio
import com.example.cambiateappkmp.presentation.permisos.rememberSolicitudUbicacion
import org.koin.compose.viewmodel.koinViewModel

/** Pantalla Mapa. Al abrirse pide el permiso de ubicación; el botón Sugerir avisa con el clima (puede ser null). */
@Composable
fun MapaRoute(
    onSugerir: (ClimaActual?) -> Unit,
    viewModel: MapaViewModel = koinViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    val solicitud = rememberSolicitudUbicacion { concedido ->
        if (concedido) viewModel.permisoConcedido() else viewModel.permisoRechazado()
    }

    // Si ya había dado el permiso, se carga directo; si no, se lo pedimos al abrir.
    LaunchedEffect(Unit) {
        if (solicitud.concedido) viewModel.permisoConcedido() else solicitud.pedir()
    }

    MapaScreen(
        state = estado,
        onPedirPermiso = solicitud::pedir,
        onReintentar = viewModel::reintentar,
        onReintentarLocales = viewModel::reintentarLocales,
        onSugerir = onSugerir,
    )
}

/**
 * Contenido de la pantalla Mapa, sin barra superior propia: el título y el botón "Volver"
 * los pone quien la muestra (la navegación), así se puede usar tanto con una barra como con pestañas.
 */
@Composable
fun MapaScreen(
    state: MapaUiState,
    onPedirPermiso: () -> Unit,
    onReintentar: () -> Unit,
    onReintentarLocales: () -> Unit,
    onSugerir: (ClimaActual?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (state) {
            is MapaUiState.SinPermiso -> EstadoVacio(
                titulo = "Necesitamos tu ubicación",
                mensaje = if (state.rechazado) {
                    "Sin el permiso no podemos mostrarte en el mapa. Podés activarlo desde los ajustes del sistema."
                } else {
                    "Para mostrarte en el mapa y buscar tiendas cerca necesitamos permiso de ubicación."
                },
                textoBoton = "Dar permiso",
                onClick = onPedirPermiso,
            )
            MapaUiState.Cargando -> EstadoCargando()
            is MapaUiState.Error -> EstadoError(mensaje = state.mensaje, onReintentar = onReintentar)
            is MapaUiState.Contenido -> ContenidoMapa(
                state = state,
                onReintentarLocales = onReintentarLocales,
                onSugerir = onSugerir,
            )
        }
    }
}

@Composable
private fun ContenidoMapa(
    state: MapaUiState.Contenido,
    onReintentarLocales: () -> Unit,
    onSugerir: (ClimaActual?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // El mapa va fuera de la lista que se desplaza, para que no compitan los gestos.
        MapaView(
            ubicacion = state.ubicacion,
            locales = state.locales,
            modifier = Modifier.fillMaxWidth().height(260.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ClimaCard(
                    clima = state.clima,
                    cargando = state.cargandoClima,
                    error = state.errorClima,
                    pronostico = state.pronostico,
                )
            }
            item {
                Button(
                    onClick = { onSugerir(state.clima) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(text = "Sugerir", style = MaterialTheme.typography.labelLarge)
                }
            }
            item {
                Text(text = "Tiendas de ropa cerca", style = MaterialTheme.typography.titleMedium)
            }
            when {
                state.cargandoLocales -> item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Buscando tiendas…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                state.errorLocales != null -> item {
                    Column {
                        Text(
                            text = state.errorLocales,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = onReintentarLocales) { Text("Reintentar") }
                    }
                }
                state.locales.isEmpty() -> item {
                    Text(
                        text = "No encontramos tiendas de ropa cerca.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> items(state.locales, key = { it.id }) { local -> ItemLocal(local) }
            }
        }
    }
}

@Composable
private fun ItemLocal(local: LocalRopa) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = local.nombre, style = MaterialTheme.typography.titleSmall)
            Text(
                text = local.direccion.ifBlank { "Sin dirección" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
