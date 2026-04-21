package com.example.medisync.viewmodel

import androidx.compose.runtime.mutableStateListOf
import com.example.medisync.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medisync.data.network.GroqRequest
import com.example.medisync.data.network.GroqService
import com.example.medisync.data.network.Message
import com.example.medisync.data.repository.MedicationRepository
import com.example.medisync.ui.screens.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
