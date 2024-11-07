package com.example.myapplication.screens.gpt

import com.example.myapplication.screens.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class RecipeRepository {

    private val firestore = FirebaseFirestore.getInstance()

    fun saveRecipe(recipe: Recipe, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val recipeData = hashMapOf(
            "name" to recipe.name,
            "ingredients" to recipe.ingredients,
            "description" to recipe.description,
            "time" to recipe.time
        )

        userId?.let {
            firestore.collection("users").document(it)
                .collection("saved_recipes").add(recipeData)
                .addOnSuccessListener {
                    Log.d("RecipeRepository", "Recipe saved successfully in Firestore.")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.w("RecipeRepository", "Error saving recipe in Firestore", e)
                    onFailure(e)
                }
        }
    }
}