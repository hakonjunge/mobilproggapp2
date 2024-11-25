package com.example.myapplication.screens.gpt

import android.util.Log
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

@OptIn(BetaOpenAI::class)
class GPTService {

    private var openAI: OpenAI? = null
    private val firebaseDatabase = FirebaseDatabase.getInstance("https://culinaire-d7287-default-rtdb.europe-west1.firebasedatabase.app/")

    suspend fun getRecipeResponse(query: String): String? {
        if (openAI == null) {
            initializeOpenAI()
        }

        return try {
            val request = ChatCompletionRequest(
                model = ModelId("gpt-4"),
                messages = listOf(ChatMessage(role = ChatRole.User, content = query))
            )
            val response = openAI?.chatCompletion(request)
            response?.choices?.firstOrNull()?.message?.content
        } catch (e: Exception) {
            Log.e("GPTService", "Error fetching GPT response: ${e.message}", e)
            null
        }
    }

    private suspend fun initializeOpenAI() {
        try {
            // Hent API-nøkkelen fra Firebase
            val apiKeySnapshot = firebaseDatabase.getReference("api_keys/openai").get().await()
            val apiKey = apiKeySnapshot.getValue(String::class.java)

            if (!apiKey.isNullOrEmpty()) {
                openAI = OpenAI(OpenAIConfig(token = apiKey))
                Log.d("GPTService", "OpenAI initialized successfully.")
            } else {
                Log.e("GPTService", "API Key is null or empty.")
            }
        } catch (e: Exception) {
            Log.e("GPTService", "Error initializing OpenAI: ${e.message}", e)
        }
    }
}