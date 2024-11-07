package com.example.myapplication.screens


import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.screens.ui.theme.MyApplicationTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavHostController

data class Dinner(val description: String, val imageUrl: String)

class DinnerListActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var capturedImages by mutableStateOf<List<Bitmap>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController() // Create NavController

                // Set up the NavHost with NavController and routes
                NavHost(navController = navController, startDestination = "dinnerList") {
                    composable("dinnerList") { DinnerListScreen(navController) }
                    composable("login") { LoginScreen(navController) }
                }
            }
        }

        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val bitmap = result.data?.extras?.get("data") as? Bitmap
                    bitmap?.let {
                        capturedImages = capturedImages + it // Add new image to the list
                    }
                }
            }
    }


    private fun uploadImageToFirebase(bitmap: Bitmap, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg") // Unik identifikator for bildet

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val uploadTask = imageRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString()) // Returner URL-en
            }
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }


    @Composable
    fun FullScreenImageDialog(
        bitmap: Bitmap,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false), // Make it take full width
            title = null,
            text = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Full-Screen Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp) // Adjust height as needed
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }


    @Composable
    fun DinnerListScreen(navController: NavHostController) {
        var dinners by remember { mutableStateOf<List<Dinner>>(emptyList()) }
        var showDialog by remember { mutableStateOf(false) }
        var selectedImage by remember { mutableStateOf<Bitmap?>(null) } // State for the selected image

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val activeIcon = remember { mutableStateOf("dinner") }
        val context = LocalContext.current

        // Fetch dinners from Firestore
        LaunchedEffect(Unit) {
            userId?.let {
                firestore.collection("dinners")
                    .whereEqualTo("userId", it)
                    .get()
                    .addOnSuccessListener { documents ->
                        dinners = documents.map { doc ->
                            Dinner(
                                description = doc.getString("description") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: ""
                            ).also {
                                Log.d("DinnerList", "Fetched dinner: ${it.description}, Image URL: ${it.imageUrl}")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("DinnerList", "Error fetching dinners: $e")
                    }
            }
        }

        // Wrapping everything in a Box to allow overlaying the IconButton
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .align(Alignment.TopCenter) // Align column content at the top
            ) {
                // Title for the screen
                Text(
                    text = "Mine Middager",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // LazyColumn for scrolling images, showing the newest first by reversing the list
                LazyColumn(
                    modifier = Modifier.weight(1f), // Use weight to allow LazyColumn to take available space
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedImages.reversed()) { bitmap -> // Reverse the list here
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Captured Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp) // Set a fixed height for the images
                                .padding(4.dp)
                                .clickable {
                                    selectedImage = bitmap // Set selected image
                                    showDialog = true // Show the dialog
                                }
                        )
                    }
                    // Add a Spacer at the end to create extra scroll space
                    item {
                        Spacer(modifier = Modifier.height(120.dp)) // Adjust height as needed
                    }
                }
            }

            // Floating + Icon at the bottom center, overlaid on top of content
            IconButton(
                onClick = { openCamera(context) },
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Aligns at the bottom center of the screen
                    .padding(bottom = 90.dp) // Adjust padding as needed
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Dinner",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Bottom navigation bar fixed at the bottom
            BottomNavigationBar(
                navController = navController,
                activeIcon = activeIcon,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth() // Make sure it fills the entire width
                    .height(80.dp)
            )

            // Show the full-screen image dialog if the selected image is set
            if (showDialog && selectedImage != null) {
                FullScreenImageDialog(bitmap = selectedImage!!) {
                    showDialog = false // Close the dialog
                    selectedImage = null // Clear the selected image
                }
            }
        }
    }


    private fun openCamera(context: Context) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    @Composable
    fun BottomNavigationBar(
        navController: NavHostController,
        activeIcon: MutableState<String>,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current // Get the current context for creating an Intent

        Row(
            modifier = modifier
                .background(MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Menu Icon for going back to Activity2
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = if (activeIcon.value == "activity") Color.White else Color.Gray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        activeIcon.value = "activity"
                        navController.navigate("activity") // Navigate to Activity2
                    }
            )

            // Dinner List Icon, white while on DinnerList
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Dinner",
                tint = if (activeIcon.value == "dinner") Color.White else Color.Gray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        activeIcon.value = "dinner" // Set active icon
                    }
            )

            // Settings Icon for navigating to Login activity
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Logg ut",
                tint = if (activeIcon.value == "setting") MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        // Sign out the user
                        FirebaseAuth.getInstance().signOut()

                        // Create and launch an intent to open the Login activity
                        val loginIntent = Intent(context, Login::class.java)
                        context.startActivity(loginIntent)

                        // Clear the activity stack to prevent returning to the previous screen
                        (context as? Activity)?.finishAffinity()
                    }
            )
        }
    }
}