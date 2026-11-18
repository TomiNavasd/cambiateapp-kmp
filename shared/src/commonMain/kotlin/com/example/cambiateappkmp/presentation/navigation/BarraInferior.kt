package com.example.cambiateappkmp.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private class Pestana(val titulo: String, val ruta: Ruta)

private val pestanas = listOf(
    Pestana("Armario", Ruta.Armario),
    Pestana("Outfits", Ruta.Outfits()),
    Pestana("Mapa", Ruta.Mapa),
)

/**
 * Barra inferior con Armario, Outfits y Mapa. No hay librería de íconos en el proyecto, así que solo lleva texto.
 * En pantallas anchas los tres ítems quedan agrupados en el centro (como mucho 560 dp) en vez de estirarse.
 */
@Composable
fun BarraInferior(rutaActual: Ruta, onPestana: (Ruta) -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().background(NavigationBarDefaults.containerColor),
        contentAlignment = Alignment.TopCenter,
    ) {
        NavigationBar(modifier = Modifier.widthIn(max = 560.dp)) {
            pestanas.forEach { pestana ->
                NavigationBarItem(
                    selected = rutaActual.mismaPestanaQue(pestana.ruta),
                    onClick = { onPestana(pestana.ruta) },
                    icon = {},
                    label = { Text(pestana.titulo) },
                    alwaysShowLabel = true,
                )
            }
        }
    }
}
