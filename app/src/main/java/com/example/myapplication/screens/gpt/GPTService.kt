package com.example.myapplication.screens.gpt

import android.util.Log
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.myapplication.BuildConfig

@OptIn(BetaOpenAI::class)
class GPTService {

    // Use BuildConfig to get the API key securely
    private val openAI = OpenAI(BuildConfig.OPENAI_API_KEY)

    suspend fun getRecipeResponse(query: String): String? {
        return try {
            Log.d("GPTService", "Using API Key: ${BuildConfig.OPENAI_API_KEY}")
            val request = ChatCompletionRequest(
                model = ModelId("gpt-4"),
                messages = listOf(ChatMessage(role = ChatRole.User, content = query))
            )
            val response = openAI.chatCompletion(request)
            response.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            Log.e("GPTService", "Error fetching GPT response: ${e.message}")
            null
        }
    }
}
