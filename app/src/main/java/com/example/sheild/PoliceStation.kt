package com.example.sheild

data class PoliceStation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    var distance: Double = 0.0
)