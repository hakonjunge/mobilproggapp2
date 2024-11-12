package com.example.myapplication.screens.gpt

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R

class GPTViewModel : ViewModel() {

    private val gptService = GPTService() // Direkte instansiering av GPTService

    private val _gptQuery = MutableStateFlow("")
    val gptQuery: StateFlow<String> = _gptQuery.asStateFlow()

    private val _gptResponse = MutableStateFlow("")  // Leave empty initially
    val gptResponse: StateFlow<String> = _gptResponse.asStateFlow()

    private val firebaseDatabase = FirebaseDatabase.getInstance("https://culinaire-d7287-default-rtdb.europe-west1.firebasedatabase.app/")
    private val auth = FirebaseAuth.getInstance()

    fun initializeResponse(context: Context) {
        // Sets the localized initial response
        _gptResponse.value = context.getString(R.string.no_recipe_found)
    }

    fun fetchRecipe(context: Context, ingredients: List<String>, time: Int, allergies: String?) {
        val allergyText = if (!allergies.isNullOrBlank()) {
            " ${context.getString(R.string.allergies_optional)}: $allergies."
        } else ""

        val query = """
        Find a recipe that uses some of the following ingredients from my fridge and pantry: ${ingredients.joinToString(", ")}.
        The recipe should be ready within $time minutes. Note: it’s not necessary to use every ingredient listed.
        Assume I have basic ingredients like salt, pepper, and oil available at home.$allergyText
        
        Please respond in JSON format with the fields:
        - 'name' (string): the name of the recipe.
        - 'ingredients' (array of strings): each ingredient as a separate string.
        - 'description' (array of strings): each step as a plain instruction without any numbering or prefixes like "Step 1" or "1.".
        - 'time' (integer): the preparation time in minutes.
    
        When generating the recipe, please provide the instructions in the same language as the ingredient list given.
        """.trimIndent()

        viewModelScope.launch {
            _gptQuery.value = query
            val response = gptService.getRecipeResponse(query)

            if (response != null) {
                _gptResponse.value = formatRecipeText(context, response)
                saveRecipeToRealtimeDatabase(response)
            } else {
                _gptResponse.value = context.getString(R.string.no_recipe_found)
            }
        }
    }


    private fun formatRecipeText(context: Context, jsonString: String): String {
        return try {
            val jsonObject = JSONObject(jsonString)
            val name = jsonObject.getString("name")
            val time = jsonObject.optInt("time", -1)
            val ingredients = jsonObject.getJSONArray("ingredients")
            val description = jsonObject.getJSONArray("description")

            val formattedText = StringBuilder()
            formattedText.append("${context.getString(R.string.recipe)}: $name\n")

            if (time != -1) {
                formattedText.append("${context.getString(R.string.time)}: $time ${context.getString(R.string.minutes)}\n\n")
            }

            formattedText.append("${context.getString(R.string.ingredients)}:\n")
            for (i in 0 until ingredients.length()) {
                formattedText.append("- ${ingredients.getString(i)}\n")
            }

            formattedText.append("\n${context.getString(R.string.description)}:\n")
            for (i in 0 until description.length()) {
                // Remove any initial numbering like "1. ", "2. " etc.
                val stepText = description.getString(i).replace(Regex("^\\d+\\.\\s*"), "")
                formattedText.append("${i + 1}. $stepText\n") // Add consistent numbering here
            }

            formattedText.toString()
        } catch (e: Exception) {
            Log.e("GPTViewModel", "Error parsing JSON: ${e.message}")
            context.getString(R.string.no_recipe_found)
        }
    }

    private fun saveRecipeToRealtimeDatabase(response: String) {
        val userId = auth.currentUser?.uid ?: return
        val recipeRef = firebaseDatabase.getReference("users/$userId/saved_recipes")

        try {
            val recipeData = JSONObject(response)
            recipeRef.push().setValue(recipeData.toString())
                .addOnSuccessListener {
                    Log.d("GPTViewModel", "Recipe saved successfully in Realtime Database.")
                }
                .addOnFailureListener { e ->
                    Log.w("GPTViewModel", "Error saving recipe in Realtime Database", e)
                }
        } catch (e: Exception) {
            Log.e("GPTViewModel", "Error formatting JSON response for Firebase: ${e.message}")
        }
    }
}