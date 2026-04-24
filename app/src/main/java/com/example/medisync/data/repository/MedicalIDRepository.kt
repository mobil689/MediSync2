package com.example.medisync.data.repository

import com.example.medisync.data.model.Contact
import com.example.medisync.data.model.MedicalID
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object MedicalIDRepository {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val collection = firestore.collection("medical_ids")

    fun getMedicalID(): Flow<MedicalID?> = callbackFlow {
        val userId = auth.currentUser?.uid ?: run {
            trySend(null)
            return@callbackFlow
        }

        val listener = collection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(MedicalID::class.java))
            } else {
                trySend(createMockData(userId))
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveMedicalID(medicalID: MedicalID) {
        val userId = auth.currentUser?.uid ?: return
        collection.document(userId).set(medicalID.copy(userId = userId, lastUpdated = System.currentTimeMillis())).await()
    }

    private fun createMockData(userId: String): MedicalID {
        return MedicalID(
            userId = userId,
            bloodType = "O+",
            allergies = listOf("Penicillin", "Peanuts", "Shellfish"),
            medications = listOf("Lisinopril", "Metformin"),
            emergencyContacts = listOf(
                Contact("Sarah Johnson", "Wife", "555-0123"),
                Contact("Dr. Robert Smith", "Primary Doctor", "555-0987")
            ),
            organDonorStatus = true,
            insuranceProvider = "Blue Cross Blue Shield",
            lastUpdated = System.currentTimeMillis()
        )
    }
}
