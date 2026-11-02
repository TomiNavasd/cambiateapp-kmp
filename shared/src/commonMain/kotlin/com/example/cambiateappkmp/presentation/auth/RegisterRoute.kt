package com.example.cambiateappkmp.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterRoute(
    onIrALogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    RegisterScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onIrALogin = onIrALogin,
        modifier = modifier,
    )
}