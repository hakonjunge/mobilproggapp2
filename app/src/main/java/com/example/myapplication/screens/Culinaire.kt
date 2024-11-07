package com.example.myapplication.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.gpt.GPTViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


class Culinaire : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CulinaireNavigation()
            }
        }
    }
}

@Composable
fun CulinaireNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "culinaire") {
        composable("culinaire") { CulinaireScreen(navController = navController) }
    }
}

@Composable
fun CulinaireScreen(navController: NavHostController, viewModel: GPTViewModel = viewModel()) {
    var selectedIngredients by remember { mutableStateOf("") }
    var allergiesInfo by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf(0f) }

    // Collect the GPT response from the ViewModel
    val gptResponse by viewModel.gptResponse.collectAsState(initial = "Ingen oppskrift funnet.")
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activeIcon = remember { mutableStateOf("menu") } // Track active icon for bottom menu

    // UI Layout
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Velg ingredienser og tid",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Time Slider
            Text(
                text = "Tid: ${selectedTime.toInt()} min",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Slider(
                value = selectedTime,
                onValueChange = { selectedTime = it },
                valueRange = 0f..60f,
                steps = 59,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // TextField for Available Ingredients
            OutlinedTextField(
                value = selectedIngredients,
                onValueChange = { selectedIngredients = it },
                label = { Text("Tilgjengelige Ingredienser") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // TextField for Allergies (Optional)
            OutlinedTextField(
                value = allergiesInfo,
                onValueChange = { allergiesInfo = it },
                label = { Text("Allergier eller Intoleranser (valgfritt)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Button to fetch recipe from GPT
            Button(onClick = {
                val ingredients = selectedIngredients.split(",").map { it.trim() }
                coroutineScope.launch {
                    viewModel.fetchRecipe(ingredients, selectedTime.toInt(), allergiesInfo)
                }
            }) {
                Text("Finn oppskrift", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display GPT response
            Text(
                text = gptResponse ?: "Ingen oppskrift funnet.",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Bottom Menu Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = if (activeIcon.value == "menu") Color.White else Color.Gray,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { activeIcon.value = "menu" }
                )
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = if (activeIcon.value == "profile") MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            activeIcon.value = "profile"
                            startDinnerListActivity(context)
                        }
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Logg ut",
                    tint = if (activeIcon.value == "settings") MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("culinaire") { inclusive = true }
                            }
                        }
                )
            }
        }
    }
}


fun startDinnerListActivity(context: Context) {
    val intent = Intent(context, DinnerListActivity::class.java)
    context.startActivity(intent)
}

@Composable
fun IngredientSelectionRow(
    title: String,
    ingredientList: List<String>,
    selectedIngredient: String?,
    onIngredientSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
        ) {
            Text(
                text = selectedIngredient ?: "Velg $title",
                modifier = Modifier.padding(8.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ingredientList.forEach { ingredient ->
                DropdownMenuItem(
                    text = { Text(ingredient) },
                    onClick = {
                        onIngredientSelected(ingredient)
                        expanded = false
                    }
                )
            }
        }
    }
}