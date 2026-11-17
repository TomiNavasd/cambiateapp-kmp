package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.fake.FakeOutfitsRepository
import com.example.cambiateappkmp.domain.repository.OutfitsRepository
import com.example.cambiateappkmp.domain.usecase.AlternarFavoritoUseCase
import com.example.cambiateappkmp.domain.usecase.DeleteOutfitUseCase
import com.example.cambiateappkmp.domain.usecase.ObserveOutfitsUseCase
import com.example.cambiateappkmp.domain.usecase.SaveOutfitUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

// Versión real: el OutfitsRepository lo aporta cada plataforma (GitLive en Android; en escritorio todavía no está disponible).
val outfitsModule = module {
    includes(outfitsPlataformaModule)
    casosDeUsoDeOutfits()
}

val outfitsFakeModule = module {
    single<OutfitsRepository> { FakeOutfitsRepository() }
    casosDeUsoDeOutfits()
}

private fun Module.casosDeUsoDeOutfits() {
    factory { ObserveOutfitsUseCase(get()) }
    factory { SaveOutfitUseCase(get()) }
    factory { DeleteOutfitUseCase(get()) }
    factory { AlternarFavoritoUseCase(get()) }
}
