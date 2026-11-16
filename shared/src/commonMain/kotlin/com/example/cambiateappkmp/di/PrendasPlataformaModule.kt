package com.example.cambiateappkmp.di

import org.koin.core.module.Module

/**
 * Registra el PrendaRepository real de cada plataforma: GitLive en Android y la API REST de Firestore en escritorio.
 * Los casos de uso son los mismos en las dos.
 */
expect val prendasPlataformaModule: Module
