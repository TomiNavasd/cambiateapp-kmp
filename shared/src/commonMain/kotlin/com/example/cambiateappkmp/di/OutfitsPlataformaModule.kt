package com.example.cambiateappkmp.di

import org.koin.core.module.Module

/**
 * Registra el OutfitsRepository real de cada plataforma: GitLive en Android y, en escritorio, una versión mínima
 * que todavía no está disponible. Los casos de uso son los mismos en las dos.
 */
expect val outfitsPlataformaModule: Module
