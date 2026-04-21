package com.example.medisync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medisync.data.model.Medication
import com.example.medisync.data.repository.MedicationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MedicationViewModel : ViewModel() {
    private val repository = MedicationRepository() // Normally injected via Hilt

    val medications: StateFlow<List<Medication>> = repository.medications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addMedication(name: String, dosage: String, times: List<String>) {
        times.forEach { timeStr ->
            val medication = Medication(
                name = name,
                dosage = dosage,
                time = timeStr,
                timeOfDay = when {
                    timeStr.contains("AM") -> {
                        val h = timeStr.split(":")[0].toInt()
                        if (h in 5..11) "morning" else "night"
                    }
                    timeStr.contains("PM") -> {
                        val h = timeStr.split(":")[0].toInt()
                        when (h) {
                            12 -> "afternoon"
                            in 1..4 -> "afternoon"
                            in 5..8 -> "evening"
                            else -> "night"
                        }
                    }
                    else -> "morning"
                }
            )
            repository.addMedication(medication)
        }
    }

    fun deleteMedications(ids: Set<String>) {
        repository.deleteMedications(ids)
    }

    fun toggleMedication(id: String) {
        repository.toggleMedication(id)
    }
}
