package com.beetik.quinielamalenkamexico2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.beetik.quinielamalenkamexico2026.ui.screens.MainScreen
import com.beetik.quinielamalenkamexico2026.ui.theme.QuinielaMalenkaMexico2026Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuinielaMalenkaMexico2026Theme {
                MainScreen()
            }
        }
    }
}
