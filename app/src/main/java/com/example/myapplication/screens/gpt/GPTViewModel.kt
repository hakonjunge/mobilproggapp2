package com.example.myapplication.screens.gpt

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

class GPTViewModel : ViewModel() {

    private val gptService = GPTService() // Direkte instansiering av GPTService

    private val _gptQuery = MutableStateFlow("")
    val gptQuery: StateFlow<String> = _gptQuery.asStateFlow()

    private val _gptResponse = MutableStateFlow("Ingen oppskrift funnet.")
    val gptResponse: StateFlow<String> = _gptResponse.asStateFlow()

    private val firebaseDatabase = FirebaseDatabase.getInstance("https://culinaire-d7287-default-rtdb.europe-west1.firebasedatabase.app/")
    private val auth = FirebaseAuth.getInstance()

    fun fetchRecipe(ingredients: List<String>, time: Int, allergies: String?) {
        // Lag en tekst for allergier hvis det er spesifisert
        val allergyText = if (!allergies.isNullOrBlank()) {
            " Exclude any ingredients that may cause the following allergies or intolerances: $allergies."
        } else ""

        // Oppdater forespørselen med en kommentar om ingrediensene og basisvarer
        val query = """
        Find a recipe that uses some of the following ingredients from my fridge and pantry: ${ingredients.joinToString(", ")}.
        The recipe should be ready within $time minutes. Note: it’s not necessary to use every ingredient listed.
        Assume I have basic ingredients like salt, pepper, and oil available at home.$allergyText
        Please respond in JSON format with the fields 'name' (string), 'ingredients' (array of strings), 
        and 'description' (array of strings describing the steps).
        When generating the recipe, please provide the instructions in the same language as the ingredient list given    
        """.trimIndent()

        viewModelScope.launch {
            _gptQuery.value = query
            val response = gptService.getRecipeResponse(query)

            if (response != null) {
                // Vis lesbar tekst til brukeren
                _gptResponse.value = formatRecipeText(response)
                // Lagre rå JSON til Firebase
                saveRecipeToRealtimeDatabase(response)
            } else {
                _gptResponse.value = "Ingen oppskrift funnet."
            }
        }
    }

    // Hjelpefunksjon for å formatere JSON-oppkrift til lesbar tekst
    private fun formatRecipeText(jsonString: String): String {
        return try {
            // Parse JSON-strengen til et JSONObject
            val jsonObject = JSONObject(jsonString)

            // Hent verdier fra JSON-objektet
            val name = jsonObject.getString("name")
            val ingredients = jsonObject.getJSONArray("ingredients")
            val description = jsonObject.getJSONArray("description")

            // Bygg opp teksten for å vise den som en lesbar oppskrift
            val formattedText = StringBuilder()
            formattedText.append("Oppskrift: $name\n\n")
            formattedText.append("Ingredienser:\n")
            for (i in 0 until ingredients.length()) {
                formattedText.append("- ${ingredients.getString(i)}\n")
            }
            formattedText.append("\nBeskrivelse:\n")
            for (i in 0 until description.length()) {
                formattedText.append("${i + 1}. ${description.getString(i)}\n")
            }

            // Returner den formatterte oppskrifts teksten
            formattedText.toString()
        } catch (e: Exception) {
            Log.e("GPTViewModel", "Kunne ikke lese JSON-oppbyggingen: ${e.message}")
            "Kunne ikke lese oppskriftens format."
        }
    }
    private fun saveRecipeToRealtimeDatabase(response: String) {
        val userId = auth.currentUser?.uid ?: return
        val recipeRef = firebaseDatabase.getReference("users/$userId/saved_recipes")

        try {
            // Lag JSON-objekt fra responsen
            val recipeData = JSONObject(response)

            // Send JSON-objektet direkte til Firebase
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