package com.example.soleilracingproject.features.NavHost

import kotlinx.serialization.Serializable

interface Route {

    @Serializable
    data object Hud: Route

    @Serializable
    data object Analysis: Route

    @Serializable
    data object Garage: Route

    @Serializable
    data object Homepage: Route

    @Serializable
    data object Disclaimer: Route

    @Serializable
    data object Login: Route

}