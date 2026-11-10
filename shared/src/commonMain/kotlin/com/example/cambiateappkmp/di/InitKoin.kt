package com.example.cambiateappkmp.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(usarFake: Boolean = false, appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            if (usarFake) authFakeModule else authModule,
            if (usarFake) prendasFakeModule else prendasModule,
            if (usarFake) imagenesFakeModule else imagenesModule,
            presentationModule,
        )
    }
}