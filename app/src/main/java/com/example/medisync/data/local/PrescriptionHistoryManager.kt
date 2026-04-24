package com.example.medisync.data.local

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.example.medisync.data.model.PrescriptionScan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrescriptionHistoryManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("prescription_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun saveScan(scan: PrescriptionScan) = withContext(Dispatchers.IO) {
        try {
            val scans = getAllScans().toMutableList()
            scans.add(0, scan)
            
            // Limit to 20 scans
            val limitedScans = if (scans.size > 20) {
                scans.take(20)
            } else {
                scans
            }
            
            val json = gson.toJson(limitedScans)
            sharedPreferences.edit { putString("scans", json) }
        } catch (e: Exception) {
            Log.e("PrescriptionHistoryManager", "Error saving scan", e)
        }
    }

    suspend fun getAllScans(): List<PrescriptionScan> = withContext(Dispatchers.IO) {
        try {
            val json = sharedPreferences.getString("scans", null)
            if (json.isNullOrEmpty()) return@withContext emptyList<PrescriptionScan>()
            
            val type = object : TypeToken<List<PrescriptionScan>>() {}.type
            val scans: List<PrescriptionScan> = gson.fromJson(json, type) ?: emptyList()
            
            // Return sorted by date newest first (though saveScan already adds to index 0)
            scans.sortedByDescending { it.scanDate }
        } catch (e: Exception) {
            Log.e("PrescriptionHistoryManager", "Error loading scans", e)
            emptyList()
        }
    }

    suspend fun deleteScan(id: String) = withContext(Dispatchers.IO) {
        try {
            val scans = getAllScans().toMutableList()
            scans.removeAll { it.id == id }
            val json = gson.toJson(scans)
            sharedPreferences.edit { putString("scans", json) }
        } catch (e: Exception) {
            Log.e("PrescriptionHistoryManager", "Error deleting scan", e)
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        sharedPreferences.edit { remove("scans") }
    }
}
