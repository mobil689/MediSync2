package com.example.medisync.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.runtime.mutableStateListOf
import com.example.medisync.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medisync.data.network.ContentItem
import com.example.medisync.data.network.GroqRequest
import com.example.medisync.data.network.GroqService
import com.example.medisync.data.network.ImageUrl
import com.example.medisync.data.network.Message
import com.example.medisync.data.repository.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream

data class ExtractedMedication(
    val name: String,
    val dose: String,
    val frequency: Int,
    val times: List<String> // "09:00", "21:00" etc.
)

data class ChatMessage(
    val id: String,
    val role: String, // "user" or "ai"
    val text: String,
    val imageBitmap: Bitmap? = null,
    val extractedMedications: List<ExtractedMedication>? = null,
    var medicationsAdded: Boolean = false
)

class TriageViewModel(
    private val repository: MedicationRepository = MedicationRepository
) : ViewModel() {
    private val apiKey = "Bearer ${BuildConfig.GROQ_API_KEY}"
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(GroqService::class.java)

    private val _messages = mutableStateListOf(
        ChatMessage("intro", "ai", "Hi, I'm your Triage assistant. Tap any area on the body, or type below to describe what's going on.")
    )
    val messages: List<ChatMessage> get() = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping = _isTyping.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var queryCount = 0
    private val conversationHistory = mutableListOf<Message>()

    private suspend fun getSystemPrompt(): String {
        val medContext = repository.getAiMedicationContext()
        return """
            You are a professional medical triage assistant. 
            TONE: Short, empathetic, and curious. Ask ONE question at a time and wait for the user.
            
            USER CONTEXT: $medContext
            CURRENT QUERY COUNT: $queryCount
            
            THE FUNNEL:
            - Turns 1–3: Focus only on clarifying symptoms. Suggest ONLY non-medicinal relief (RICE method, hydration, specific light stretches).
            - Turns 4–5: Provide a 'Preliminary Observation' and recommend a specific specialist (e.g., Orthopedic, Neurologist, ENT).
            
            SAFETY REDLINES:
            - STRICTLY FORBID naming any pharmaceutical drugs (No Ibuprofen, No Aspirin, etc.).
            - If user mentions chest pressure, difficulty breathing, or sudden numbness, trigger an IMMEDIATE Bold Emergency Warning to seek urgent care.
            - NEVER provide a final diagnosis. ALWAYS use empathetic, hedging language like 'might' or 'could'.
        """.trimIndent()
    }

    fun startGeneralTriage() {
        processUserQuery("general wellness")
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _messages.add(ChatMessage("u-${System.currentTimeMillis()}", "user", text))
        conversationHistory.add(Message("user", text))
        queryCount++
        executeAiCall()
    }

    fun selectZone(zone: String) {
        val userText = "I have pain in my $zone."
        _messages.add(ChatMessage("u-${System.currentTimeMillis()}", "user", userText))
        conversationHistory.add(Message("user", userText))
        queryCount++
        executeAiCall()
    }

    fun uploadPrescription(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _isTyping.value = true
            
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
                
                launch(Dispatchers.Main) {
                    _messages.add(ChatMessage(
                        id = "u-img-${System.currentTimeMillis()}",
                        role = "user",
                        text = "Uploaded a prescription image.",
                        imageBitmap = bitmap
                    ))
                    _messages.add(ChatMessage(
                        id = "ai-loading-${System.currentTimeMillis()}",
                        role = "ai",
                        text = "Analyzing prescription..."
                    ))
                }

                val prompt = "Analyze this medical prescription image. Extract and list: 1. All medication names 2. Dosage for each medication 3. Frequency (how many times a day) 4. Duration (how many days) 5. Any special instructions (take with food, etc.) Format the response clearly. If the image is not a prescription, say so."
                
                val content = listOf(
                    ContentItem(type = "text", text = prompt),
                    ContentItem(type = "image_url", imageUrl = ImageUrl(url = "data:image/jpeg;base64,$base64String"))
                )
                
                val request = GroqRequest(
                    model = "meta-llama/llama-4-scout-17b-16e-instruct",
                    messages = listOf(Message("user", content)),
                    maxTokens = 1024
                )
                
                val response = service.getTriageQuestions(apiKey, request)
                val aiResponse = response.choices.firstOrNull()?.message?.content 
                    ?: "I'm sorry, I couldn't process that request right now."
                
                launch(Dispatchers.Main) {
                    // Remove the "Analyzing..." message
                    if (_messages.isNotEmpty() && _messages.last().text == "Analyzing prescription...") {
                        _messages.removeAt(_messages.size - 1)
                    }
                    _messages.add(ChatMessage("ai-${System.currentTimeMillis()}", "ai", aiResponse))
                    
                    // Step 1: Second API call to extract structured data
                    extractStructuredMedications(aiResponse)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    if (_messages.isNotEmpty() && _messages.last().text == "Analyzing prescription...") {
                        _messages.removeAt(_messages.size - 1)
                    }
                    _messages.add(ChatMessage("ai-${System.currentTimeMillis()}", "ai", "Couldn't read this image. Try taking a clearer photo."))
                }
            } finally {
                _isLoading.value = false
                _isTyping.value = false
            }
        }
    }

    private fun extractStructuredMedications(analysisText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = """
                    From the following prescription analysis, extract medications as a JSON array ONLY. No other text.
                    Format: [{"name": "Medicine Name", "dose": "dosage string", "frequency": 2, "times": ["09:00", "21:00"]}]
                    - "frequency" is how many times per day
                    - "times" is a suggested array of times in 24hr HH:mm format, spaced evenly across waking hours (08:00 to 22:00)
                    - If frequency is 1, use ["09:00"]. If 2, use ["09:00", "21:00"]. If 3, use ["08:00", "14:00", "21:00"]
                    Prescription analysis:
                    $analysisText
                """.trimIndent()

                val request = GroqRequest(
                    model = "llama-3.1-8b-instant",
                    messages = listOf(Message("user", prompt)),
                    maxTokens = 512
                )

                val response = service.getTriageQuestions(apiKey, request)
                val jsonText = response.choices.firstOrNull()?.message?.content ?: ""
                
                // Simple regex to extract JSON array if AI included other text
                val jsonMatch = Regex("\\[.*]").find(jsonText.replace("\n", ""))?.value

                if (jsonMatch != null) {
                    val jsonArray = org.json.JSONArray(jsonMatch)
                    val extractedList = mutableListOf<ExtractedMedication>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val times = mutableListOf<String>()
                        val timesArray = obj.getJSONArray("times")
                        for (j in 0 until timesArray.length()) {
                            times.add(timesArray.getString(j))
                        }
                        extractedList.add(ExtractedMedication(
                            name = obj.getString("name"),
                            dose = obj.getString("dose"),
                            frequency = obj.getInt("frequency"),
                            times = times
                        ))
                    }

                    if (extractedList.isNotEmpty()) {
                        launch(Dispatchers.Main) {
                            // Find the last AI message and attach extracted medications
                            val lastIndex = _messages.indexOfLast { it.role == "ai" }
                            if (lastIndex != -1) {
                                val original = _messages[lastIndex]
                                _messages[lastIndex] = original.copy(extractedMedications = extractedList)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markMedicationsAdded(messageId: String) {
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            _messages[index] = _messages[index].copy(medicationsAdded = true)
        }
    }

    fun processUserQuery(query: String) {
        val userText = if (query == "general wellness") "I want to start a general triage checkup." else "I have an issue with my $query."
        _messages.add(ChatMessage("u-${System.currentTimeMillis()}", "user", userText))
        conversationHistory.add(Message("user", userText))
        queryCount++
        executeAiCall()
    }

    private fun executeAiCall() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _isTyping.value = true
            try {
                val systemMessage = Message("system", getSystemPrompt())
                val apiMessages = listOf(systemMessage) + conversationHistory
                
                val request = GroqRequest(
                    model = "llama-3.1-8b-instant",
                    messages = apiMessages
                )
                
                val response = service.getTriageQuestions(apiKey, request)
                val aiResponse = response.choices.firstOrNull()?.message?.content 
                    ?: "I'm sorry, I couldn't process that request right now."
                
                conversationHistory.add(Message("assistant", aiResponse))
                
                launch(Dispatchers.Main) {
                    _messages.add(ChatMessage("ai-${System.currentTimeMillis()}", "ai", aiResponse))
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    _messages.add(ChatMessage("ai-${System.currentTimeMillis()}", "ai", "Error: ${e.localizedMessage}"))
                }
            } finally {
                _isLoading.value = false
                _isTyping.value = false
            }
        }
    }
}
