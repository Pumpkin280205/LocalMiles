
package com.example.localmiles.Screens.Auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import com.example.localmiles.Core.Auth.BiometricAuthManager
import com.example.localmiles.Core.Utils.BiometricPreferences
import com.example.localmiles.R

@Composable
fun SplashScreen(
    onResolved: (role: String?) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current

    var authenticationRequired by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        val role = viewModel.currentRoleOrNull()

        if (role == null) {
            onResolved(null)
            return@LaunchedEffect
        }

        if (!BiometricPreferences.isEnabled(context)) {
            onResolved(role)
            return@LaunchedEffect
        }

        val activity = context as? FragmentActivity

        if (activity == null) {
            authenticationRequired = true
            return@LaunchedEffect
        }

        BiometricAuthManager(activity).authenticate { success, _ ->
            if (success) {
                onResolved(role)
            } else {
                authenticationRequired = true
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (authenticationRequired) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.authentication_required),
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = stringResource(R.string.authentication_login_message),
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = {
                        onResolved(null)
                    }
                ) {
                    Text(stringResource(R.string.continue_to_login))
                }
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
