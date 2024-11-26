package com.example.myapplication.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.ui.theme.MyApplicationTheme

class ViewOldRecipe : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val database =
        FirebaseDatabase.getInstance("https://culinaire-d7287-default-rtdb.europe-west1.firebasedatabase.app/")
    val userId = auth.currentUser?.uid
    var recipes by remember { mutableStateOf<List<SavedRecipe>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedRecipe by remember { mutableStateOf<SavedRecipe?>(null) }
    var recipeToDelete by remember { mutableStateOf<SavedRecipe?>(null) }
    val context = LocalContext.current

    // Fetch recipes from Firebase
    LaunchedEffect(Unit) {
        if (userId == null) {
            errorMessage = context.getString(R.string.no_user)
            Log.d("ViewOldRecipeScreen", errorMessage ?: "No user error message available")
        } else {
            database.getReference("users/$userId/saved_recipes").get()
                .addOnSuccessListener { dataSnapshot ->
                    val recipeList = mutableListOf<SavedRecipe>()
                    dataSnapshot.children.forEach { child ->
                        val jsonString = child.getValue(String::class.java)
                        val key = child.key ?: return@forEach
                        jsonString?.let {
                            try {
                                val jsonObject = JSONObject(it)
                                val name = jsonObject.optString("name")
                                val ingredients = jsonObject.optJSONArray("ingredients")?.let { jsonArray ->
                                    List(jsonArray.length()) { index ->
                                        jsonArray.optString(index) ?: ""
                                    }
                                } ?: emptyList()
                                val description = jsonObject.optJSONArray("description")?.let { jsonArray ->
                                    List(jsonArray.length()) { index ->
                                        jsonArray.optString(index) ?: ""
                                    }
                                } ?: emptyList()
                                val time = jsonObject.optInt("time", 0)
                                recipeList.add(SavedRecipe(key, name, ingredients, description, time))
                            } catch (e: Exception) {
                                Log.e("ViewOldRecipeScreen", "Error parsing JSON: ${e.message}")
                            }
                        }
                    }
                    recipes = recipeList
                }
                .addOnFailureListener {
                    errorMessage = "${context.getString(R.string.fetch_error)} ${it.message}"
                }
        }
    }

    // Set the background color based on the current theme
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    // Main layout with background
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (selectedRecipe == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val culinaireIntent = Intent(context, Culinaire::class.java)
                        context.startActivity(culinaireIntent)
                        (context as? Activity)?.finish()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(id = R.string.previous_recipes),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                color = textColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
            )
        }

        if (selectedRecipe != null) {
            RecipeDetailScreen(
                recipe = selectedRecipe!!,
                onBack = { selectedRecipe = null }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(recipes) { recipe ->
                    RecipeItem(
                        recipe = recipe,
                        onClick = { selectedRecipe = recipe },
                        onDelete = { recipeToDelete = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (recipeToDelete != null) {
                DeleteRecipeDialog(
                    recipe = recipeToDelete!!,
                    onConfirm = {
                        deleteRecipeFromFirebase(userId, database, recipeToDelete!!.key)
                        recipes = recipes.filter { it.key != recipeToDelete!!.key }
                        recipeToDelete = null
                    },
                    onDismiss = { recipeToDelete = null }
                )
            }
        }
    }
}
@Composable
fun RecipeItem(recipe: SavedRecipe, onClick: () -> Unit, onDelete: (SavedRecipe) -> Unit) {
    val textColor = MaterialTheme.colorScheme.onBackground // Dynamisk tekstfarge basert på tema

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp,
                    color = textColor // Tilpass tekstfargen til temaet
                )
            )
            Text(
                text = "${stringResource(id = R.string.time)}: ${recipe.time} ${stringResource(id = R.string.minutes)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    color = textColor.copy(alpha = 0.7f) // Gjør teksten litt lysere
                )
            )
        }

        IconButton(onClick = { onDelete(recipe) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(id = R.string.delete),
                tint = Color.Red // Sett slett-knappen til å være konstant rød
            )
        }
    }
}

@Composable
fun DeleteRecipeDialog(
    recipe: SavedRecipe,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.delete_confirmation)) },
        text = { Text(text = stringResource(id = R.string.delete_message, recipe.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.no))
            }
        }
    )
}

fun deleteRecipeFromFirebase(userId: String?, database: FirebaseDatabase, recipeKey: String) {
    if (userId != null) {
        val reference = database.getReference("users/$userId/saved_recipes/$recipeKey")
        reference.removeValue()
            .addOnSuccessListener {
                Log.d("DeleteRecipe", "Recipe with key $recipeKey deleted successfully.")
            }
            .addOnFailureListener {
                Log.e("DeleteRecipe", "Failed to delete recipe: ${it.message}")
            }
    }
}


@Composable
fun RecipeDetailScreen(recipe: SavedRecipe, onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                shareRecipe(context, recipe)
            }) {
                Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(id = R.string.share))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 0.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        modifier = Modifier.size(32.dp)
                    )
                }
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

            Text(
                text = "${stringResource(id = R.string.ingredients)}:",
                style = MaterialTheme.typography.bodyLarge
            )
            recipe.ingredients.forEach { ingredient ->
                Text(text = "- $ingredient", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${stringResource(id = R.string.description)}:",
                style = MaterialTheme.typography.bodyLarge
            )
            recipe.description.forEachIndexed { index, step ->
                Text(text = "${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

fun shareRecipe(context: Context, recipe: SavedRecipe) {
    val shareContent = """
        ${context.getString(R.string.recipe)}: ${recipe.name}
        ${context.getString(R.string.time)}: ${recipe.time} ${context.getString(R.string.minutes)}
        ${context.getString(R.string.ingredients)}:
        ${recipe.ingredients.joinToString(separator = "\n") { "- $it" }}
        
        ${context.getString(R.string.description)}:
        ${recipe.description.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")}
    """.trimIndent()

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareContent)
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
}


data class SavedRecipe(
    val key: String = "", // Add a key for the Firebase node
    val name: String = "",
    val ingredients: List<String> = emptyList(),
    val description: List<String> = emptyList(),
    val time: Int = 0
)

