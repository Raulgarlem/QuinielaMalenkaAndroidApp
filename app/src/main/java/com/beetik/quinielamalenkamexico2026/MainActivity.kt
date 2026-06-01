package com.beetik.quinielamalenkamexico2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.beetik.quinielamalenkamexico2026.ui.screens.QuinielaScreen
import com.beetik.quinielamalenkamexico2026.ui.theme.QuinielaMalenkaMexico2026Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuinielaMalenkaMexico2026Theme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    QuinielaScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
