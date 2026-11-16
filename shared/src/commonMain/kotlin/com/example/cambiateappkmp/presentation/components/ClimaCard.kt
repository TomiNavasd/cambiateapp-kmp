package com.example.cambiateappkmp.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cambiateappkmp.domain.model.ClimaActual
import com.example.cambiateappkmp.domain.model.PronosticoManiana
import kotlin.math.roundToInt

/**
 * Tarjeta con el clima. Muestra el clima actual, un indicador de lluvia en texto y,
 * si se le pasa, el pronóstico de mañana. Tiene estados de carga, error y sin datos.
 */
@Composable
fun ClimaCard(
    clima: ClimaActual?,
    cargando: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    pronostico: PronosticoManiana? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Clima", style = MaterialTheme.typography.labelLarge)

            when {
                clima != null -> ClimaActualContenido(clima)
                cargando -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Buscando el clima…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error != null -> Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                pronostico == null -> Text(
                    text = "Sin datos del clima por ahora.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (pronostico != null) PronosticoContenido(pronostico)
        }
    }
}

@Composable
private fun ClimaActualContenido(clima: ClimaActual) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "${clima.temperaturaC.roundToInt()} °C",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(text = clima.descripcion, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (clima.llueve) "Está lloviendo" else "Sin lluvia",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PronosticoContenido(pronostico: PronosticoManiana) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Mañana · ${pronostico.minimaC.roundToInt()}° / ${pronostico.maximaC.roundToInt()}°",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "${pronostico.descripcion} · ${pronostico.probabilidadLluvia}% de probabilidad de lluvia",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
