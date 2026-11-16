package com.example.cambiateappkmp

import com.example.cambiateappkmp.domain.config.ApiKeys

class AndroidApiKeys : ApiKeys {
    override val anthropic: String = BuildConfig.ANTHROPIC_API_KEY

    // Sin uso por ahora: el mapa es OpenStreetMap (osmdroid) y no necesita clave.
    override val maps: String = ""
}
