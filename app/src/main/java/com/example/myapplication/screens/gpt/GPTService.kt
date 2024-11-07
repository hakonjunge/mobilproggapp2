package com.example.myapplication.screens.gpt

import android.util.Log
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import javax.inject.Inject

@OptIn(BetaOpenAI::class)
class GPTService @Inject constructor() {

    private val openAI = OpenAI(CHAT_GPT_API_KEY)

    suspend fun getRecipeResponse(query: String): String? {
        return try {
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

    companion object {
        private const val CHAT_GPT_API_KEY = "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"
    }
}