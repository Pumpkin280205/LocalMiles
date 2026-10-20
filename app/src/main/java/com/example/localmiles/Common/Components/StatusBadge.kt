package com.example.localmiles.Common.Components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Job / stop status pill shared by every role's screens. Colours come from the
 * theme so the same status looks the same everywhere, in light and dark mode.
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor) = statusColors(status)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Text(
            text = formatStatus(status),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Background/content colours for a status — also used for map pins and legends. */
@Composable
fun statusColors(status: String): Pair<Color, Color> {
    val colorScheme = MaterialTheme.colorScheme

    return when (status.lowercase().replace(" ", "")) {
        "completed", "delivered" -> colorScheme.primary to colorScheme.onPrimary
        "enroute", "inprogress", "arrived" -> colorScheme.secondary to colorScheme.onSecondary
        "assigned" -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        "pending" -> colorScheme.tertiary to colorScheme.onTertiary
        "cancelled", "failed" -> colorScheme.error to colorScheme.onError
        else -> colorScheme.surfaceVariant to colorScheme.onSurfaceVariant
    }
}

/** "EnRoute" -> "En Route", "InProgress" -> "In Progress". */
fun formatStatus(status: String): String =
    status.replace(Regex("([a-z])([A-Z])"), "$1 $2")
