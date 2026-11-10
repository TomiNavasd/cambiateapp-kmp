package com.example.cambiateappkmp.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudinaryRespuesta(
    @SerialName("secure_url") val secureUrl: String,
)
