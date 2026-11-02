package com.example.cambiateappkmp.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoute(
    onIrARegistro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LoginScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onIrARegistro = onIrARegistro,
        modifier = modifier,
    )
}