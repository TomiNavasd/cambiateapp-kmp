package com.example.cambiateappkmp

import android.app.Application
import com.example.cambiateappkmp.di.initKoin
import com.example.cambiateappkmp.domain.config.ApiKeys
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class CambiateApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(usarFake = USAR_AUTH_FAKE) {
            androidContext(this@CambiateApplication)
            modules(module { single<ApiKeys> { AndroidApiKeys() } })
        }
    }

    private companion object {
        // Único lugar donde se alterna el auth.
        // true: usa FakeAuthRepository en memoria (demo@cambiateapp.com / 123456), sin Firebase.
        // false: usa AuthRepositoryImpl con Firebase real.
        const val USAR_AUTH_FAKE = false
    }
}
