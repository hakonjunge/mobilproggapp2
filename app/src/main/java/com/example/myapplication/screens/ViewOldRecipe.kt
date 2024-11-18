package com.example.myapplication.screens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.screens.ui.theme.MyApplicationTheme

class ViewOldRecipe : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Ensure the activity fills the entire screen
        setContent {
            MyApplicationTheme {
                ViewOldRecipeScreen()
            }
        }
    }
}

@Composable
fun ViewOldRecipeScreen() {
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://culinaire-d7287-default-rtdb.europe-west1.firebasedatabase.app/")
    val userId = auth.currentUser?.uid
    var recipes by remember { mutableStateOf<List<SavedRecipe>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRecipe by remember { mutableStateOf<SavedRecipe?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (userId == null) {
            errorMessage = context.getString(R.string.no_user)
            Log.d("ViewOldRecipeScreen", errorMessage ?: "No user error message available")
        } else {
            Log.d("ViewOldRecipeScreen", "User logged in with UID: $userId. Attempting to fetch data.")
            database.getReference("users/$userId/saved_recipes").get()
                .addOnSuccessListener { dataSnapshot ->
                    val recipeList = mutableListOf<SavedRecipe>()
                    dataSnapshot.children.forEach { child ->
                        val jsonString = child.getValue(String::class.java)
                        jsonString?.let {
                            try {
                                val jsonObject = JSONObject(it)
                                val name = jsonObject.optString("name")
                                val ingredients = jsonObject.optJSONArray("ingredients")?.let { jsonArray ->
                                    List(jsonArray.length()) { index -> jsonArray.optString(index) ?: "" }
                                } ?: emptyList()
                                val description = jsonObject.optJSONArray("description")?.let { jsonArray ->
                                    List(jsonArray.length()) { index -> jsonArray.optString(index) ?: "" }
                                } ?: emptyList()
                                val time = jsonObject.optInt("time", 0)
                                recipeList.add(SavedRecipe(name, ingredients, description, time))
                            } catch (e: Exception) {
                                Log.e("ViewOldRecipeScreen", "Error parsing JSON: ${e.message}")
                            }
                        }
                    }
                    recipes = recipeList
                }
                .addOnFailureListener {
                    errorMessage = "${context.getString(R.string.fetch_error)} ${it.message}"
                    Log.e("ViewOldRecipeScreen", errorMessage ?: "Unknown fetch error")
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back button for navigating back to Culinaire
        if (selectedRecipe == null) {
            IconButton(
                onClick = {
                    val culinaireIntent = Intent(context, Culinaire::class.java)
                    context.startActivity(culinaireIntent)
                    (context as? Activity)?.finish() // Close ViewOldRecipe if necessary
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        }

        if (selectedRecipe != null) {
            RecipeDetailScreen(recipe = selectedRecipe!!, onBack = { selectedRecipe = null })
        } else {
            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: context.getString(R.string.unknown_error),
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp)) // Add some space at the top
                    }

                    items(recipes) { recipe ->
                        RecipeItem(recipe) {
                            selectedRecipe = recipe
                        }
                        Divider()
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp)) // Add more space at the bottom
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeItem(recipe: SavedRecipe, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Text(
            text = recipe.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp)
        )
        Text(
            text = "${stringResource(id = R.string.time)}: ${recipe.time} ${stringResource(id = R.string.minutes)}",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            color = Color.Gray
        )
    }
}

@Composable
fun RecipeDetailScreen(recipe: SavedRecipe, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.back))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${stringResource(id = R.string.recipe)}: ${recipe.name}",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp)
        )
        Text(
            text = "${stringResource(id = R.string.time)}: ${recipe.time} ${stringResource(id = R.string.minutes)}",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "${stringResource(id = R.string.ingredients)}:", style = MaterialTheme.typography.bodyLarge)
        recipe.ingredients.forEach { ingredient ->
            Text(text = "- $ingredient", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "${stringResource(id = R.string.description)}:", style = MaterialTheme.typography.bodyLarge)
        recipe.description.forEachIndexed { index, step ->
            Text(text = "${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

data class SavedRecipe(
    val name: String = "",
    val ingredients: List<String> = emptyList(),
    val description: List<String> = emptyList(),
    val time: Int = 0
)


