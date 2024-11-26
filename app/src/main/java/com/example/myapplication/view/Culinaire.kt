package com.example.myapplication.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.myapplication.backend.GPTViewModel
import com.example.myapplication.notifications.ReminderReceiver
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.backend.Navigation
import java.text.SimpleDateFormat
import java.util.*
import com.example.myapplication.backend.Navigation.*

class Culinaire : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS permission for Android 13+ devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CulinaireScreen(navController: NavHostController, viewModel: GPTViewModel = viewModel()) {
    val context = LocalContext.current

    // Check for first launch of the day and show notification
    LaunchedEffect(Unit) {
        checkFirstLaunchOfDay(context)
    }

    var selectedIngredients by remember { mutableStateOf("") }
    var allergiesInfo by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf(0f) }
    val activeIcon = remember { mutableStateOf("menu") }

    val gptResponse by viewModel.gptResponse.collectAsState(initial = "")
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CulinAire",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(56.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                                    viewModel.fetchRecipe(
                                        context,
                                        ingredients,
                                        selectedTime.toInt(),
                                        allergiesInfo
                                    )
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
                                Navigation.startViewOldRecipeActivity(context)
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
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
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
                            .size(36.dp)
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
                                Navigation.startDinnerListActivity(context)
                            }
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (activeIcon.value == "settings") MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                Navigation.startSettingsActivity(context)
                            }
                    )
                }
            }
        }
    }
}



fun checkFirstLaunchOfDay(context: Context) {
    val sharedPreferences = context.getSharedPreferences("CulinairePrefs", Context.MODE_PRIVATE)
    val lastLaunchDate = sharedPreferences.getString("lastLaunchDate", null)
    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    if (lastLaunchDate != currentDate) {
        sharedPreferences.edit().putString("lastLaunchDate", currentDate).apply()
        ReminderReceiver().sendNotification(context)
    }
}