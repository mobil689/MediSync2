package com.example.medisync.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medisync.data.model.MedicalID
import com.example.medisync.data.repository.MedicalIDRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicalIDViewModel(
    private val repository: MedicalIDRepository = MedicalIDRepository
) : ViewModel() {
    private val _medicalID = MutableStateFlow<MedicalID?>(null)
    val medicalID = _medicalID.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadMedicalID()
    }

    private fun loadMedicalID() {
        viewModelScope.launch {
            repository.getMedicalID().collect { id ->
                _medicalID.value = id
                _isLoading.value = false
            }
        }
    }

    fun updateMedicalID(newID: MedicalID) {
        viewModelScope.launch {
            repository.saveMedicalID(newID)
        }
    }
}
