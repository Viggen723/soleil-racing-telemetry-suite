package com.example.soleilracingproject.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey val id: String,
    val name: String,
    val make: String,
    val model: String,
    val year: Int? = null,
    val specifications: String? = null,
    val modifications: String? = null,
    val photoPath: String? = null
)
