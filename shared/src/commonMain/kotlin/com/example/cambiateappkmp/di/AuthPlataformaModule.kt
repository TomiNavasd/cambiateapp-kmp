package com.example.cambiateappkmp.di

import org.koin.core.module.Module

/**
 * Registra el AuthRepository real de cada plataforma: GitLive en Android y la API REST de Firebase en escritorio.
 * Los casos de uso son los mismos en las dos.
 */
expect val authPlataformaModule: Module
