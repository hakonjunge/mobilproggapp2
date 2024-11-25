package com.example.myapplication.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment

class Settings : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                SettingsScreen() // This is the settings screen for your UI
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity  // Cast the context to Activity

    Scaffold(
        topBar = {
            // Replace TopAppBar with custom Surface and set background color to gray
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary  // Set the top bar's background to gray
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp) // Add padding around the title
                ) {
                    // Back arrow icon aligned to the left
                    IconButton(
                        onClick = { activity?.finish() }, // Close current activity
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack, // Use built-in arrow back icon
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    // Settings title centered in the Box
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        },
        bottomBar = {
            BottomBar() // This is the button to log out
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Add any settings UI elements here (like checkboxes, switches, etc.)
                Text(
                    text = "Configure your preferences here.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
        }
    )
}

@Composable
fun BottomBar() {
    val context = LocalContext.current // Get context within composable

    Button(
        onClick = {
            logOutAndNavigateToLogin(context)  // Pass context here
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp),

        ) {
        Text("Log Out", fontSize = 18.sp)
    }
}

fun logOutAndNavigateToLogin(context: Context) {
    // Log out using Firebase Authentication
    FirebaseAuth.getInstance().signOut()

    // Navigate to the Login activity
    val intent = Intent(context, Login::class.java)
    context.startActivity(intent)
}


