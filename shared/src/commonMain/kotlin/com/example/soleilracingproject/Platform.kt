package com.example.soleilracingproject

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform