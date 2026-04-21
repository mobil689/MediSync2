package com.example.medisync.data.model

import java.util.UUID

data class Medication(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dosage: String,
    val time: String, // HH:mm format
    val timeOfDay: String, // "morning", "afternoon", "evening", "night"
    val days: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    val isTaken: Boolean = false,
    val loggedTime: String? = null
)
