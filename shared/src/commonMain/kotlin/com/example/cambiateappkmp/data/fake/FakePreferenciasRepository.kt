package com.example.cambiateappkmp.data.fake

import com.example.cambiateappkmp.data.preferences.AlmacenClaveValorEnMemoria
import com.example.cambiateappkmp.data.repository.PreferenciasRepositoryImpl
import com.example.cambiateappkmp.domain.repository.PreferenciasRepository

/** Preferencias en memoria: se pierden al cerrar la app. */
class FakePreferenciasRepository :
    PreferenciasRepository by PreferenciasRepositoryImpl(AlmacenClaveValorEnMemoria())
