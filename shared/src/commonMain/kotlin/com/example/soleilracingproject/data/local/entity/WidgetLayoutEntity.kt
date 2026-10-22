package com.example.soleilracingproject.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

// One row only
@Entity(tableName = "widget_layout")
data class WidgetLayoutEntity(
    @PrimaryKey val id: String,
    val widgets: String
)