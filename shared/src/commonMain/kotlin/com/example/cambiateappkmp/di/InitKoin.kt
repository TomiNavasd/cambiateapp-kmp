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
            if (usarFake) climaFakeModule else climaModule,
            if (usarFake) ubicacionFakeModule else ubicacionModule,
            if (usarFake) mapaFakeModule else mapaModule,
            if (usarFake) ajustesFakeModule else ajustesModule,
            if (usarFake) notificacionesFakeModule else notificacionesModule,
            if (usarFake) outfitsFakeModule else outfitsModule,
            if (usarFake) iaFakeModule else iaModule,
            presentationModule,
        )
    }
}