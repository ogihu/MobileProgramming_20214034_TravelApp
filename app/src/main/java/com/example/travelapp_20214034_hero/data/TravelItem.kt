package com.example.travelapp_20214034_hero.data

data class TravelItem(
    val id: Long = 0,
    val place: String,
    val visitDate: String,
    val memo: String = "",
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
