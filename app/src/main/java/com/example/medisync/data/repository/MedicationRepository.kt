package com.example.medisync.data.repository

import com.example.medisync.data.model.Medication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository handling medication data.
 * Currently uses in-memory mock data, but ready for Firebase integration.
 */
class MedicationRepository {
    private val _medications = MutableStateFlow<List<Medication>>(
        listOf(
            Medication(name = "Lisinopril", dosage = "10 mg", time = "8:00 AM", timeOfDay = "morning", isTaken = false),
            Medication(name = "Metformin", dosage = "500 mg", time = "1:00 PM", timeOfDay = "afternoon", isTaken = true, loggedTime = "1:04 PM"),
            Medication(name = "Atorvastatin", dosage = "20 mg", time = "7:30 AM", timeOfDay = "morning", isTaken = false)
        )
    )
    val medications: Flow<List<Medication>> = _medications.asStateFlow()

    fun addMedication(medication: Medication) {
        _medications.value = _medications.value + medication
    }

    fun deleteMedications(ids: Set<String>) {
        _medications.value = _medications.value.filter { it.id !in ids }
    }

    fun toggleMedication(id: String) {
        _medications.value = _medications.value.map {
            if (it.id == id) {
                val newState = !it.isTaken
                it.copy(
                    isTaken = newState,
                    loggedTime = if (newState) "Just now" else null
                )
            } else it
        }
    }
}
