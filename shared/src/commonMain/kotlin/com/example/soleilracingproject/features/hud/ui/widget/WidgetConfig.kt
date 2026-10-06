package com.example.soleilracingproject.features.hud.ui.widget

data class WidgetConfig(
    val id: String,
    val content: WidgetContent,
    val title: String,
    val isVisible: Boolean = true,
    val span: WidgetSpan = WidgetSpan.HALF,
    val heightDp: Int = 160
)
