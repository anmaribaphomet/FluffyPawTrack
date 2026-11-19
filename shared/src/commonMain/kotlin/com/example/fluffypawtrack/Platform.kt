package com.example.fluffypawtrack

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform