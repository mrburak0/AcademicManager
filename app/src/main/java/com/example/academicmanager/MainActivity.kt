package com.example.academicmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.academicmanager.ui.MainScreen
import com.example.academicmanager.ui.theme.AcademicManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AcademicManagerTheme {
                MainScreen()
            }
        }
    }
}
