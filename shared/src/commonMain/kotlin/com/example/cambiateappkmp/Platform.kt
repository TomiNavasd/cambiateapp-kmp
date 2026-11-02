package com.example.cambiateappkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform