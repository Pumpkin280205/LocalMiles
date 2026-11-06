package com.example.localmiles.Screens.Driver.Sync

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Shows pending / failed / synced status. Hidden until something has been queued
@Composable
fun SyncStatusBanner(summary: SyncSummary, onRetryFailed: () -> Unit, onDismissFailed: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val (text, container, content) = when {
        summary.pending > 0 -> Triple(
            "${summary.pending} change(s) saved offline, waiting to sync",
            colors.tertiaryContainer, colors.onTertiaryContainer,
        )
        summary.failed > 0 -> Triple(
            "${summary.failed} change(s) were rejected by the server",
            colors.errorContainer, colors.onErrorContainer,
        )
        summary.synced > 0 -> Triple("All changes synced", colors.primaryContainer, colors.onPrimaryContainer)
        else -> return
    }
    Surface(color = container, contentColor = content, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (summary.pending == 0 && summary.failed > 0) {
                TextButton(onClick = onRetryFailed) { Text("Retry") }
                TextButton(onClick = onDismissFailed) { Text("Dismiss") }
            }
        }
    }
}
