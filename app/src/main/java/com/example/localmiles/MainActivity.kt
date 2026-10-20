package com.example.localmiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.localmiles.Core.Navigation.NavGraph
import com.example.localmiles.Core.Notifications.PushTokenRegistrar
import com.example.localmiles.Core.Utils.DispatcherPreferences
import com.example.localmiles.Core.Utils.DriverPreferences
import com.example.localmiles.Core.Utils.ThemePreferences
import com.example.localmiles.Core.theme.LocalMilesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreferences.init(this)
        DispatcherPreferences.init(this)
        DriverPreferences.init(this)
        // Already signed in: re-send the FCM token (it can change between launches).
        if (savedInstanceState == null) PushTokenRegistrar.register(this)
        enableEdgeToEdge()
        setContent {
            LocalMilesTheme {
                NavGraph()
            }
        }
    }
}
