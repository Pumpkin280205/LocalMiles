package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatcherSettingsScreen(
    viewModel: DispatcherSettingsViewModel,
    onBack: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var firstName by rememberSaveable {
        mutableStateOf("")
    }

    var lastName by rememberSaveable {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    LaunchedEffect(uiState.user) {

        uiState.user?.let { user ->

            firstName = user.firstName
            lastName = user.lastName
        }
    }

    Scaffold(
        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(
                    rememberScrollState()
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Dispatcher Account",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Update your personal account information."
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (uiState.isLoading) {

                CircularProgressIndicator()

            } else {

                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("First Name")
                    },
                    singleLine = true,
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = {
                        lastName = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Last Name")
                    },
                    singleLine = true,
                    enabled = !uiState.isSaving
                )

                OutlinedTextField(
                    value = uiState.user?.email ?: "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Email")
                    },
                    singleLine = true,
                    enabled = false
                )

                OutlinedTextField(
                    value = uiState.user?.role ?: "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Role")
                    },
                    singleLine = true,
                    enabled = false
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = {
                        viewModel.updateProfile(
                            firstName = firstName,
                            lastName = lastName
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {

                    if (uiState.isSaving) {

                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp)
                        )

                    } else {

                        Text("Save Changes")
                    }
                }

                uiState.successMessage?.let { message ->

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = message,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                }

                uiState.errorMessage?.let { message ->

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = message,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}