package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.data.ai.ClaudeAnalizadorDePrenda
import com.example.cambiateappkmp.data.ai.ClaudeOutfitSuggester
import com.example.cambiateappkmp.data.ai.ReglasOutfitSuggester
import com.example.cambiateappkmp.data.ai.SugeridorConRespaldo
import com.example.cambiateappkmp.data.ai.crearClienteClaude
import com.example.cambiateappkmp.data.fake.FakeAnalizadorDePrenda
import com.example.cambiateappkmp.data.fake.FakeOutfitSuggester
import com.example.cambiateappkmp.domain.repository.AnalizadorDePrenda
import com.example.cambiateappkmp.domain.repository.OutfitSuggester
import com.example.cambiateappkmp.domain.usecase.AnalizarPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.EtiquetarPrendaUseCase
import com.example.cambiateappkmp.domain.usecase.SuggestOutfitsUseCase
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

// Versión real: Claude, con las reglas simples de respaldo para los outfits. La clave sale solo de ApiKeys.anthropic
// (la aporta cada app).
val iaModule = module {
    single(named("claude")) { crearClienteClaude() }
    single<OutfitSuggester> {
        SugeridorConRespaldo(
            principal = ClaudeOutfitSuggester(apiKeys = get(), cliente = get(named("claude"))),
            respaldo = ReglasOutfitSuggester(),
        )
    }
    single<AnalizadorDePrenda> { ClaudeAnalizadorDePrenda(apiKeys = get(), cliente = get(named("claude"))) }
    casosDeUsoDeIa()
}

// Versión para pruebas: sin red y sin clave.
val iaFakeModule = module {
    single<OutfitSuggester> { FakeOutfitSuggester() }
    single<AnalizadorDePrenda> { FakeAnalizadorDePrenda() }
    casosDeUsoDeIa()
}

private fun Module.casosDeUsoDeIa() {
    factory { SuggestOutfitsUseCase(get(), get()) }
    factory { AnalizarPrendaUseCase(get()) }
    factory { EtiquetarPrendaUseCase(get()) }
}
