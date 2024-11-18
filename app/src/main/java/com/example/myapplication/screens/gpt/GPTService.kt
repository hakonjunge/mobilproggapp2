package com.example.myapplication.screens.gpt

import android.util.Log
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig

@OptIn(BetaOpenAI::class)
class GPTService {

    // Direkte hardkodet API-nøkkel (ikke anbefalt for produksjon)
    private val openAI = OpenAI(OpenAIConfig(token = "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"))
    suspend fun getRecipeResponse(query: String): String? {
        val apiUrl = "https://api.openai.com/v1/chat/completions"  // Explicitly state the URL for logging
        return try {
            Log.d("GPTService", "Using hardcoded API Key.")
            Log.d("GPTService", "Attempting to connect to OpenAI endpoint: $apiUrl")

            val request = ChatCompletionRequest(
                model = ModelId("gpt-4"),
                messages = listOf(ChatMessage(role = ChatRole.User, content = query))
            )
            val response = openAI.chatCompletion(request)
            response.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            Log.e("GPTService", "Error fetching GPT response: ${e.message}", e)
            null
        }
    }
}