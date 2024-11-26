import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") // Correct plugin ID
}

val properties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Use the secret key
        buildConfigField("String", "OPENAI_API_KEY", "\"${properties["OPENAI_API_KEY"]}\"")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true // Enable BuildConfig feature
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.ktor.client.okhttp)
    implementation(libs.openai.client)
    implementation(libs.coil.compose)
    implementation(libs.gson)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.core.ktx.v160)
    implementation(libs.androidx.appcompat.v131)
    implementation(libs.androidx.activity.compose.v131)
    implementation(libs.androidx.ui.v105)
    implementation(libs.androidx.material3.v100alpha02)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.ktx.v240)
    implementation(libs.androidx.navigation.compose.v240beta01)
    implementation(libs.firebase.auth.ktx.v2200)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.material)
    implementation(libs.androidx.core.ktx.v1120)
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.storage)
    implementation(libs.firebase.firestore)
    implementation(libs.google.firebase.auth)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.json)
    implementation(libs.androidx.material3.v100alpha03)
    implementation(libs.material3)
    implementation(libs.ui)
    implementation(libs.androidx.activity.compose.vlatestversion)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.lifecycle.runtime.ktx.v251)
    implementation(libs.androidx.lifecycle.viewmodel.compose.v251)
    implementation (libs.androidx.datastore.preferences)
}