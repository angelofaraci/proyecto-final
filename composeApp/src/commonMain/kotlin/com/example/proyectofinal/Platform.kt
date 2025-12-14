package com.example.proyectofinal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform