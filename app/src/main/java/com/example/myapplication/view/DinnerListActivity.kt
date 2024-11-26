package com.example.myapplication.view

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.R
import com.example.myapplication.backend.Navigation
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

class DinnerListActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private var capturedImages by mutableStateOf<List<String>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSavedImages()

        setContent {
            MyApplicationTheme {
                DinnerListScreen()
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                bitmap?.let {
                    val imagePath = saveBitmapToFile(this, it)
                    if (imagePath != null) {
                        capturedImages = capturedImages + imagePath
                        saveImagesToStorage(capturedImages)
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

        return try {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            }
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun loadSavedImages() {
        val user = FirebaseAuth.getInstance().currentUser
        val sharedPreferences = getSharedPreferences("image_storage", Context.MODE_PRIVATE)

        if (user != null) {
            val userKey = "image_paths_${user.uid}"
            val imagePaths = sharedPreferences.getStringSet(userKey, emptySet()) ?: emptySet()
            capturedImages = imagePaths.toList()
        } else {
            capturedImages = emptyList()
        }
    }

    private fun saveImagesToStorage(images: List<String>) {
        val user = FirebaseAuth.getInstance().currentUser
        val sharedPreferences = getSharedPreferences("image_storage", Context.MODE_PRIVATE)

        if (user != null) {
            val userKey = "image_paths_${user.uid}"
            with(sharedPreferences.edit()) {
                putStringSet(userKey, images.toSet())
                apply()
            }
        }
    }

    @Composable
    fun DinnerListScreen() {
        var showDialog by remember { mutableStateOf(false) }
        var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
        val context = LocalContext.current
        val activeIcon = remember { mutableStateOf("dinner") } // Aktivt ikon i navigasjonsbaren

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter) // Innhold øverst i skjermen
            ) {
                Text(
                    text = stringResource(id = R.string.myDinner),
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
                                    onClick = { deleteImage(imagePath) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Image",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Kamera-knapp i midten, over navigasjonsbaren
            IconButton(
                onClick = { openCamera(context) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp) // Løfter den over navigasjonsbaren
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Dinner",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Navigasjonslinje nederst
            BottomNavigationBar(
                activeIcon = activeIcon,
                modifier = Modifier.align(Alignment.BottomCenter) // Festet til bunnen
            )

            // Dialog for å vise bilder i fullskjerm
            if (showDialog && selectedImage != null) {
                FullScreenImageDialog(bitmap = selectedImage!!) {
                    showDialog = false
                    selectedImage = null
                }
            }
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
    private fun deleteImage(imagePath: String) {
        val file = File(imagePath)
        if (file.exists()) file.delete()
        capturedImages = capturedImages.filter { it != imagePath }
        saveImagesToStorage(capturedImages)
    }

    private fun loadBitmapFromFile(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists()) BitmapFactory.decodeStream(FileInputStream(file)) else null
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
        activeIcon: MutableState<String>,
        modifier: Modifier = Modifier // Sørg for å inkludere dette parameteret
    ) {
        val context = LocalContext.current

        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
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
                    .size(36.dp)
                    .clickable { Navigation.startCulinaireActivity(context) }
            )
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Dinner",
                tint = if (activeIcon.value == "dinner") MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier
                    .size(36.dp)
            )
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = if (activeIcon.value == "settings") MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { Navigation.startSettingsActivity(context) }
            )
        }
    }
}