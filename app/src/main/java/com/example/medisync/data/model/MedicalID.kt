package com.example.medisync.data.model

data class MedicalID(
    val userId: String = "",
    val bloodType: String = "",
    val allergies: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val emergencyContacts: List<Contact> = emptyList(),
    val organDonorStatus: Boolean = false,
    val insuranceProvider: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class Contact(
    val name: String = "",
    val relationship: String = "",
    val phone: String = ""
)
