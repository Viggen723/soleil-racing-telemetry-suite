package com.example.soleilracingproject.model.garage

data class Car(
    val id: String,
    val name: String,
    val make: String,
    val model: String,
    val year: Int? = null,
    val specifications: String? = null,
    val modifications: String? = null,
    val photoPath: String? = null
)
