package com.example.cambiateappkmp.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.cambiateappkmp.domain.repository.ImageStorageRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Elige una foto (galería o cámara), la sube y devuelve la URL con [onFotoSubida].
 * Muestra la foto actual ([fotoUrl]) si ya hay una.
 */
@Composable
fun SelectorFoto(
    fotoUrl: String,
    onFotoSubida: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val almacenamiento: ImageStorageRepository = koinInject()
    val scope = rememberCoroutineScope()
    val alSubir by rememberUpdatedState(onFotoSubida)

    var subiendo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val selector = rememberSelectorImagen { uri ->
        error = null
        subiendo = true
        scope.launch {
            almacenamiento.subirImagen(uri)
                .onSuccess { url -> alSubir(url) }
                .onFailure { error = "No pudimos subir la foto. Probá de nuevo." }
            subiendo = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (fotoUrl.isBlank()) {
                Text(
                    text = "Sin foto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FotoPrenda(url = fotoUrl, modifier = Modifier.fillMaxSize())
            }
            if (subiendo) CircularProgressIndicator()
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { selector.abrirGaleria() },
                enabled = !subiendo,
            ) { Text("Galería") }

            if (selector.camaraDisponible) {
                OutlinedButton(
                    onClick = { selector.abrirCamara() },
                    enabled = !subiendo,
                ) { Text("Cámara") }
            }
        }

        if (error != null) {
            Text(
                text = error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}