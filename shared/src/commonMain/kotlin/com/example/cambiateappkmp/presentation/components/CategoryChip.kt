package com.example.cambiateappkmp.presentation.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CategoryChip(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(text = texto) },
        shape = CircleShape,
        modifier = modifier,
    )
}