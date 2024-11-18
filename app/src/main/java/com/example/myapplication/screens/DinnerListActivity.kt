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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
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
        enableEdgeToEdge() // Sørg for at aktiviteten fyller hele skjermen
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

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
                        capturedImages = capturedImages + it
                    }
                }
            }
    }

    private fun openCamera(context: Context) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    @Composable
    fun DinnerListScreen(navController: NavHostController) {
        var dinners by remember { mutableStateOf<List<Dinner>>(emptyList()) }
        var showDialog by remember { mutableStateOf(false) }
        var selectedImage by remember { mutableStateOf<Bitmap?>(null) }

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

        // Fullskjerm layout for DinnerListScreen
        Column(modifier = Modifier.fillMaxSize()) {
            // Title and dinner list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Dette sikrer at listen tar opp tilgjengelig plass
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Mine Middager",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // LazyColumn for scrolling images, showing the newest first by reversing the list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedImages.reversed()) { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Captured Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .padding(4.dp)
                                .clickable {
                                    selectedImage = bitmap
                                    showDialog = true
                                }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }

            // Floating Action Button for adding new images
            IconButton(
                onClick = { openCamera(context) },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 40.dp)
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

            // Bottom navigation bar
            BottomNavigationBarForDinnerList(
                activeIcon = activeIcon,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            // Full-screen image dialog
            if (showDialog && selectedImage != null) {
                FullScreenImageDialog(bitmap = selectedImage!!) {
                    showDialog = false
                    selectedImage = null
                }
            }
        }
    }
}
@Composable
fun BottomNavigationBarForDinnerList(
    activeIcon: MutableState<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // Set the height to ensure it’s consistent
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically // Center the icons vertically
    ) {
        // Menu Icon
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            tint = if (activeIcon.value == "menu") Color.White else Color.Gray,
            modifier = Modifier
                .size(36.dp) // Adjust icon size
                .clickable {
                    activeIcon.value = "menu"
                    val culinaireIntent = Intent(context, Culinaire::class.java)
                    context.startActivity(culinaireIntent)
                }
        )

        // Dinner List Icon
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Dinner",
            tint = if (activeIcon.value == "dinner") Color.White else Color.Gray,
            modifier = Modifier
                .size(36.dp)
                .clickable {
                    activeIcon.value = "dinner"
                }
        )

        // Settings Icon
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Logg ut",
            tint = if (activeIcon.value == "setting") MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier
                .size(36.dp)
                .clickable {
                    FirebaseAuth.getInstance().signOut()
                    val loginIntent = Intent(context, Login::class.java)
                    context.startActivity(loginIntent)
                    (context as? Activity)?.finishAffinity()
                }
        )
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

