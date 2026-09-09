package com.example.localmiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.localmiles.Core.Navigation.NavGraph
import com.example.localmiles.Core.theme.LocalMilesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalMilesTheme {
                NavGraph()
            }
        }
    }
}
