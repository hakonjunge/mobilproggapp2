package com.example.myapplication.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R // Importér R-filen for å kunne bruke string-ressursene


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
    val context = LocalContext.current

    var selectedIngredients by remember { mutableStateOf("") }
    var allergiesInfo by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf(0f) }
    val activeIcon = remember { mutableStateOf("menu") } // Track active icon for bottom menu

    val gptResponse by viewModel.gptResponse.collectAsState(initial = "") // Start with empty string
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.choose_ingredients_and_time),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Time Slider
            Text(
                text = "${stringResource(id = R.string.time)}: ${selectedTime.toInt()} min",
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
                label = { Text(stringResource(id = R.string.available_ingredients)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // TextField for Allergies (Optional)
            OutlinedTextField(
                value = allergiesInfo,
                onValueChange = { allergiesInfo = it },
                label = { Text(stringResource(id = R.string.allergies_optional)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Button Row for Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Button(
                        onClick = {
                            val ingredients = selectedIngredients.split(",").map { it.trim() }
                            coroutineScope.launch {
                                viewModel.fetchRecipe(context, ingredients, selectedTime.toInt(), allergiesInfo)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(stringResource(id = R.string.generate_recipe))
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Button(
                        onClick = {
                            Log.d("CulinaireScreen", "Navigating to ViewOldRecipe")
                            startViewOldRecipeActivity(context)
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(stringResource(id = R.string.view_previous_recipes))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = gptResponse,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Centered and Balanced Bottom Navigation Bar
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
                        .size(36.dp) // Set icon size
                        .clickable { activeIcon.value = "menu" }
                )
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = if (activeIcon.value == "profile") MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable {
                            activeIcon.value = "profile"
                            startDinnerListActivity(context)
                        }
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = if (activeIcon.value == "settings") MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            // Start Settings activity when the user clicks on the settings icon
                            startSettingsActivity(context)
                        }

                )
            }
        }
    }
}

// Function to start DinnerListActivity
fun startDinnerListActivity(context: Context) {
    val intent = Intent(context, DinnerListActivity::class.java)
    context.startActivity(intent)
}


fun startViewOldRecipeActivity(context: Context) {
    val intent = Intent(context, ViewOldRecipe::class.java)
    context.startActivity(intent)
}// Funksjon for å starte DinnerListActivity

fun startSettingsActivity(context: Context) {
    val intent = Intent(context, Settings::class.java)
    context.startActivity(intent)
}