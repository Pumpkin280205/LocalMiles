package com.example.localmiles.Screens.Dispatcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    activeJobsCount: Int = 0,
    driversCount: Int = 0,
    inTransitCount: Int = 0,
    issuesCount: Int = 0,
    onJobsClick: () -> Unit = {},
    onFleetClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onUsersClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {

    var accountMenuExpanded by remember {
        mutableStateOf(false)
    }

    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dispatcher",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Operations Dashboard",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },

                actions = {

                    // ------------------------------------------------
                    // Refresh button
                    // ------------------------------------------------

                    IconButton(
                        onClick = onRefresh
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }

                    // ------------------------------------------------
                    // Dispatcher account button
                    // ------------------------------------------------

                    Box {

                        IconButton(
                            onClick = {
                                accountMenuExpanded = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Account"
                            )
                        }

                        DropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = {
                                accountMenuExpanded = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("Settings")
                                },
                                onClick = {

                                    accountMenuExpanded = false

                                    onSettingsClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Logout")
                                },
                                onClick = {

                                    accountMenuExpanded = false

                                    showLogoutDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),

            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ========================================================
            // OVERVIEW
            // ========================================================

            Text(
                text = "Overview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Jobs",
                    value = activeJobsCount.toString(),
                    icon = Icons.Default.Assignment
                )

                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Drivers",
                    value = driversCount.toString(),
                    icon = Icons.Default.DirectionsCar
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "In Transit",
                    value = inTransitCount.toString(),
                    icon = Icons.Default.Map
                )

                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Issues",
                    value = issuesCount.toString(),
                    icon = Icons.Default.Warning
                )
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            // ========================================================
            // DISPATCHER TOOLS
            // ========================================================

            Text(
                text = "Dispatcher Tools",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            DashboardActionCard(
                title = "Jobs",
                description = "Create, assign and manage delivery jobs.",
                icon = Icons.Default.Assignment,
                onClick = onJobsClick
            )

            DashboardActionCard(
                title = "Fleet Monitoring",
                description = "Monitor drivers and vehicle availability.",
                icon = Icons.Default.DirectionsCar,
                onClick = onFleetClick
            )

            DashboardActionCard(
                title = "Live Map",
                description = "View active deliveries and driver locations.",
                icon = Icons.Default.Map,
                onClick = onMapClick
            )

            DashboardActionCard(
                title = "Delivery History",
                description = "Review completed deliveries and POD records.",
                icon = Icons.Default.Assignment,
                onClick = onHistoryClick
            )

            DashboardActionCard(
                title = "User Management",
                description = "Manage dispatcher, driver and client accounts.",
                icon = Icons.Default.People,
                onClick = onUsersClick
            )
        }
    }

    // ============================================================
    // LOGOUT CONFIRMATION
    // ============================================================

    if (showLogoutDialog) {

        AlertDialog(
            onDismissRequest = {
                showLogoutDialog = false
            },

            title = {
                Text(
                    text = "Logout"
                )
            },

            text = {
                Text(
                    text = "Are you sure you want to logout?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showLogoutDialog = false

                        onLogout()
                    }
                ) {
                    Text("Logout")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}