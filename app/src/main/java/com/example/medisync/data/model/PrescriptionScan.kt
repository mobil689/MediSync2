package com.example.medisync.data.model

import java.util.UUID

data class PrescriptionScan(
    val id: String = UUID.randomUUID().toString(),
    val patientName: String,        // extracted from prescription or "Unknown"
    val diagnosis: String,          // extracted diagnosis or "General"
    val medications: List<ScannedMedication>,
    val scanDate: Long = System.currentTimeMillis(), // timestamp
    val rawAiResponse: String       // full AI response text for reference
)

data class ScannedMedication(
    val name: String,
    val dose: String,
    val frequency: String,          // "1 Morning", "2 times daily", etc.
    val duration: String            // "8 Days", "3 Days", etc.
)
