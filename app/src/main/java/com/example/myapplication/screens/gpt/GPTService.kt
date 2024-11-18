package com.example.myapplication.screens.gpt

import android.util.Log
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI

@OptIn(BetaOpenAI::class)
class GPTService {

    // Direkte hardkodet API-nøkkel (ikke anbefalt for produksjon)
    private val openAI = OpenAI("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")

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