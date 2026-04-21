package com.example.medisync.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.medisync.data.model.Medication
import com.example.medisync.data.repository.MedicationRepository
import com.example.medisync.util.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TodayUiState(
    val medications: List<Medication> = emptyList(),
    val userName: String = "Abhi",
    val selectedIds: Set<String> = emptySet(),
    val isAddDrawerOpen: Boolean = false,
    val isDeleteConfirmOpen: Boolean = false
)

class MedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MedicationRepository
    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        observeMedications()
    }

    private fun observeMedications() {
        repository.medications.onEach { list ->
            _uiState.update { it.copy(medications = list) }
        }.launchIn(viewModelScope)
    }

    fun updateUserName(newName: String) {
        _uiState.update { it.copy(userName = newName) }
    }

    fun toggleAddDrawer(open: Boolean) {
        _uiState.update { it.copy(isAddDrawerOpen = open) }
    }

    fun toggleDeleteConfirm(open: Boolean) {
        _uiState.update { it.copy(isDeleteConfirmOpen = open) }
    }

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            val newSelection = state.selectedIds.toMutableSet()
            if (newSelection.contains(id)) newSelection.remove(id) else newSelection.add(id)
            state.copy(selectedIds = newSelection)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun addMedication(name: String, dosage: String, times: List<String>, days: List<String> = emptyList()) {
        times.forEach { timeStr ->
            val medication = Medication(
                name = name,
                dosage = dosage,
                time = timeStr,
                days = days,
                timeOfDay = calculateTimeOfDay(timeStr)
            )
            repository.addMedication(medication)
            AlarmScheduler.scheduleAlarm(context, medication)
        }
    }

    fun deleteSelected() {
        val selectedMedications = _uiState.value.medications.filter { it.id in _uiState.value.selectedIds }
        selectedMedications.forEach { AlarmScheduler.cancelAlarm(context, it) }

        repository.deleteMedications(_uiState.value.selectedIds)
        clearSelection()
        toggleDeleteConfirm(false)
    }

    fun toggleMedication(id: String) {
        repository.toggleMedication(id)
    }

    private fun calculateTimeOfDay(timeStr: String): String {
        return when {
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
    }
}
