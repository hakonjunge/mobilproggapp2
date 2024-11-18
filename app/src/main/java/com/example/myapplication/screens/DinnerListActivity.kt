package com.example.myapplication.screens

import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.Delete
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
import java.io.FileInputStream

data class Dinner(val description: String, val imageUrl: String)

class DinnerListActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var capturedImages by mutableStateOf<List<String>>(emptyList()) // Store file paths

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSavedImages() // Load saved images on launch

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

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
                        val imagePath = saveBitmapToFile(this, it)
                        if (imagePath != null) {
                            capturedImages = capturedImages + imagePath
                            saveImagesToStorage(capturedImages) // Save image paths
                        } else {
                            Log.e("ImageStorage", "Failed to save image.")
                        }
                    }
                }
            }
    }

    private fun saveBitmapToFile(context: Context, bitmap: Bitmap): String? {
        val filename = "IMG_${UUID.randomUUID()}.jpg"
        val dir = context.getExternalFilesDir("images")
        val file = File(dir, filename)

        var fileOutputStream: FileOutputStream? = null
        return try {
            fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            fileOutputStream?.close()
        }
    }

    private fun loadSavedImages() {
        val sharedPreferences = getSharedPreferences("image_storage", Context.MODE_PRIVATE)
        val imagePaths = sharedPreferences.getStringSet("image_paths", emptySet()) ?: emptySet()
        capturedImages = imagePaths.toList()
    }

    private fun saveImagesToStorage(images: List<String>) {
        val sharedPreferences = getSharedPreferences("image_storage", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putStringSet("image_paths", images.toSet())
            apply()
        }
    }

    @Composable
    fun FullScreenImageDialog(
        bitmap: Bitmap,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = null,
            text = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Full-Screen Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }

    fun startSettingsActivity(context: Context) {
        val intent = Intent(context, Settings::class.java)
        context.startActivity(intent)
    }

    fun startCulinaireActivity(context: Context) {
        val intent = Intent(context, Culinaire::class.java)
        context.startActivity(intent)
    }

    @Composable
    fun DinnerListScreen(navController: NavHostController) {
        var showDialog by remember { mutableStateOf(false) }
        var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
        val context = LocalContext.current

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = "Mine Middager",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(capturedImages.reversed()) { imagePath ->
                        val bitmap = loadBitmapFromFile(imagePath)
                        bitmap?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Captured Image",
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(250.dp)
                                        .clickable {
                                            selectedImage = it
                                            showDialog = true
                                        }
                                )
                                IconButton(
                                    onClick = {
                                        deleteImage(imagePath) // Slette funksjonen
                                    },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Image",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }

            IconButton(
                onClick = { openCamera(context) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
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

            BottomNavigationBar(
                navController = navController,
                activeIcon = remember { mutableStateOf("dinner") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(80.dp)
            )

            if (showDialog && selectedImage != null) {
                FullScreenImageDialog(bitmap = selectedImage!!) {
                    showDialog = false
                    selectedImage = null
                }
            }
        }
    }

    private fun deleteImage(imagePath: String) {
        val file = File(imagePath)
        if (file.exists()) {
            file.delete() // Slett filen
        }
        capturedImages = capturedImages.filter { it != imagePath } // Fjern fra listen
        saveImagesToStorage(capturedImages) // Oppdater lagret liste
    }



    private fun loadBitmapFromFile(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeStream(FileInputStream(file))
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
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
        val context = LocalContext.current

        Row(
            modifier = modifier
                .background(MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = if (activeIcon.value == "menu") Color.White else Color.Gray,
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        startCulinaireActivity(context)
                    }
            )

            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Dinner",
                tint = if (activeIcon.value == "dinner") Color.White else Color.Gray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        activeIcon.value = "dinner"
                    }
            )

            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = if (activeIcon.value == "settings") MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        startSettingsActivity(context)
                    }
            )
        }
    }
}
