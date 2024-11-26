package com.example.myapplication.backend

import android.content.Context
import android.content.Intent
import com.example.myapplication.view.Culinaire
import com.example.myapplication.view.DinnerListActivity
import com.example.myapplication.view.Login
import com.example.myapplication.view.Register
import com.example.myapplication.view.Settings
import com.example.myapplication.view.ViewOldRecipe
import com.google.firebase.auth.FirebaseAuth


class Navigation {


    companion object {
        fun startViewOldRecipeActivity(context: Context) {
            val intent = Intent(context, ViewOldRecipe::class.java)
            context.startActivity(intent)
        }
        fun startDinnerListActivity(context: Context) {
            val intent = Intent(context, DinnerListActivity::class.java)
            context.startActivity(intent)
        }

        fun startSettingsActivity(context: Context) {
            val intent = Intent(context, Settings::class.java)
            context.startActivity(intent)
        }
        fun logOutAndNavigateToLogin(context: Context) {
            // Log out using Firebase Authentication
            FirebaseAuth.getInstance().signOut()

            // Navigate to the Login activity
            val intent = Intent(context, Login::class.java)
            context.startActivity(intent)
        }

        fun startCulinaireActivity(context: Context) {
            val intent = Intent(context, Culinaire::class.java)
            context.startActivity(intent)
        }
        fun startRegisterActicity(context: Context) {
            val intent = Intent(context, Register::class.java)
            context.startActivity(intent)
        }
        fun navigateToLogin(context: Context) {
            val intent = Intent(context, Login::class.java)
            context.startActivity(intent)
        }
    }
}