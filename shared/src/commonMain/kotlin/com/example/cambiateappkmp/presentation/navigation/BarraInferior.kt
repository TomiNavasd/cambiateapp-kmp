package com.example.cambiateappkmp.presentation.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

private class Pestana(val titulo: String, val ruta: Ruta)

private val pestanas = listOf(
    Pestana("Armario", Ruta.Armario),
    Pestana("Outfits", Ruta.Outfits()),
    Pestana("Mapa", Ruta.Mapa),
)

/** Barra inferior con Armario, Outfits y Mapa. No hay librería de íconos en el proyecto, así que solo lleva texto. */
@Composable
fun BarraInferior(rutaActual: Ruta, onPestana: (Ruta) -> Unit) {
    NavigationBar {
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
