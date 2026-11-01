package com.example.localmiles

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.localmiles.Core.Navigation.NavGraph
import com.example.localmiles.Core.Notifications.PushTokenRegistrar
import com.example.localmiles.Core.Utils.DispatcherPreferences
import com.example.localmiles.Core.Utils.DriverPreferences
import com.example.localmiles.Core.Utils.ThemePreferences
import com.example.localmiles.Core.theme.LocalMilesTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemePreferences.init(this)
        DispatcherPreferences.init(this)
        DriverPreferences.init(this)

        // Already signed in: re-send the FCM token.
        if (savedInstanceState == null) {
            PushTokenRegistrar.register(this)
        }

        enableEdgeToEdge()

        setContent {
            LocalMilesTheme {
                NavGraph()
            }
        }
    }
}