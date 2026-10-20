package com.example.localmiles.Screens.Dispatcher

import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.localmiles.Common.Components.AppTopBar
import com.example.localmiles.Common.Components.ErrorState
import com.example.localmiles.Common.Components.LoadingIndicator
import com.example.localmiles.Common.Components.StatusBadge
import com.example.localmiles.Common.Components.statusColors
import com.example.localmiles.Core.Utils.Coordinates
import com.example.localmiles.Core.Utils.DispatcherPreferences
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Default view: South Africa, until there's something to zoom to.
private val SOUTH_AFRICA = GeoPoint(-29.0, 25.0)

/**
 * Dispatcher Live Map — a real OpenStreetMap map (osmdroid) showing depots,
 * the stops of every active job, the planned route between them, and each
 * driver's live GPS position (or, without a recent ping, their last confirmed
 * stop). Refreshes on the interval set in Dispatcher Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(
    viewModel: DispatcherViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by DispatcherPreferences.settings.collectAsState()

    var showDepots by remember { mutableStateOf(true) }
    var showStops by remember { mutableStateOf(true) }
    var showDrivers by remember { mutableStateOf(true) }
    var lastUpdated by remember { mutableStateOf<LocalTime?>(null) }
    // Bumped to ask the map to re-fit every marker, or set to a point to focus on.
    var fitRequest by remember { mutableStateOf(0) }
    var focusPoint by remember { mutableStateOf<Coordinates?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadDispatcherData()
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.errorMessage == null) lastUpdated = LocalTime.now()
    }

    // Auto-refresh while the screen is open (Dispatcher Settings → Live map).
    LaunchedEffect(preferences.mapAutoRefresh, preferences.mapRefreshSeconds) {
        while (preferences.mapAutoRefresh) {
            delay(preferences.mapRefreshSeconds * 1000L)
            viewModel.loadDispatcherData()
        }
    }

    val deliveries = buildActiveDeliveries(uiState)
    val activeDepots = uiState.depots.filter { it.isActive && it.coordinates().isPlottable() }

    val subtitle = buildString {
        append(lastUpdated?.let { "Updated ${it.format(DateTimeFormatter.ofPattern("HH:mm"))}" } ?: "Loading")
        if (preferences.mapAutoRefresh) append(" · every ${preferences.mapRefreshSeconds}s")
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Live Map",
                subtitle = subtitle,
                showBack = true,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.loadDispatcherData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh map")
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoading && uiState.depots.isEmpty() && uiState.jobs.isEmpty() ->
                LoadingIndicator(
                    message = "Loading map...",
                    modifier = Modifier.padding(paddingValues)
                )

            uiState.errorMessage != null && uiState.depots.isEmpty() ->
                ErrorState(
                    title = "Unable to load the map",
                    message = uiState.errorMessage ?: "Unknown error",
                    onRetry = { viewModel.loadDispatcherData() },
                    modifier = Modifier.padding(paddingValues)
                )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showDrivers,
                        onClick = { showDrivers = !showDrivers },
                        label = { Text("Drivers ${deliveries.count { it.driverPosition != null }}") },
                        leadingIcon = { LegendDot(MaterialTheme.colorScheme.tertiary) }
                    )
                    FilterChip(
                        selected = showStops,
                        onClick = { showStops = !showStops },
                        label = { Text("Stops ${deliveries.sumOf { it.stops.size }}") },
                        leadingIcon = { LegendDot(MaterialTheme.colorScheme.secondary) }
                    )
                    FilterChip(
                        selected = showDepots,
                        onClick = { showDepots = !showDepots },
                        label = { Text("Depots ${activeDepots.size}") },
                        leadingIcon = { LegendDot(MaterialTheme.colorScheme.primaryContainer, square = true) }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                ) {
                    OsmMap(
                        deliveries = deliveries,
                        depots = activeDepots,
                        showDepots = showDepots,
                        showStops = showStops,
                        showDrivers = showDrivers,
                        fitRequest = fitRequest,
                        focusPoint = focusPoint,
                        onFocusHandled = { focusPoint = null }
                    )

                    SmallFloatingActionButton(
                        onClick = { fitRequest++ },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Show everything")
                    }

                    Text(
                        text = "© OpenStreetMap contributors",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Active deliveries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (deliveries.isEmpty()) {
                        item {
                            Text(
                                text = "No jobs are assigned or en route right now. Depots are still shown on the map.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(deliveries, key = { it.job.jobId }) { delivery ->
                        ActiveDeliveryCard(
                            delivery = delivery,
                            onClick = {
                                focusPoint = delivery.driverPosition
                                    ?: delivery.stops.firstOrNull()?.coordinates()?.takeIf { it.isPlottable() }
                            }
                        )
                    }

                    if (uiState.errorMessage != null) {
                        item {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OsmMap(
    deliveries: List<ActiveDelivery>,
    depots: List<com.example.localmiles.Core.Network.Dto.DepotResponse>,
    showDepots: Boolean,
    showStops: Boolean,
    showDrivers: Boolean,
    fitRequest: Int,
    focusPoint: Coordinates?,
    onFocusHandled: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val depotColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
    val driverColor = MaterialTheme.colorScheme.tertiary.toArgb()
    val routeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f).toArgb()
    val stopColors = mapOf(
        "Pending" to statusColors("Pending").first.toArgb(),
        "Arrived" to statusColors("Arrived").first.toArgb(),
        "Completed" to statusColors("Completed").first.toArgb(),
        "Failed" to statusColors("Failed").first.toArgb(),
        "Cancelled" to statusColors("Cancelled").first.toArgb(),
    )

    val mapView = remember {
        // osmdroid needs a user agent and a writable cache before the first MapView.
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 4.0
            controller.setZoom(5.5)
            controller.setCenter(SOUTH_AFRICA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Plain holder (not Compose state) — it's written from the AndroidView update block.
    val fitTracker = remember { FitTracker() }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize(),
        update = { map ->
            map.overlays.clear()
            val allPoints = mutableListOf<GeoPoint>()

            if (showStops) {
                deliveries.forEach { delivery ->
                    val route = buildList {
                        delivery.depot?.coordinates()?.takeIf { it.isPlottable() }?.let { add(it.toGeo()) }
                        delivery.stops.map { it.coordinates() }.filter { it.isPlottable() }.forEach { add(it.toGeo()) }
                    }
                    if (route.size > 1) {
                        map.overlays.add(Polyline(map).apply {
                            setPoints(route)
                            outlinePaint.color = routeColor
                            outlinePaint.strokeWidth = 8f
                            title = "Job #${delivery.job.jobId} route"
                        })
                    }

                    delivery.stops.filter { it.coordinates().isPlottable() }.forEach { stop ->
                        val point = stop.coordinates().toGeo()
                        allPoints += point
                        map.overlays.add(Marker(map).apply {
                            position = point
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            icon = dotDrawable(stopColors[stop.status] ?: depotColor, sizePx = 34)
                            title = "Stop ${stop.sequenceNumber} · ${stop.recipientName}"
                            snippet = "Job #${delivery.job.jobId} · ${stop.status}<br>${stop.address}"
                        })
                    }
                }
            }

            if (showDepots) {
                depots.forEach { depot ->
                    val point = depot.coordinates().toGeo()
                    allPoints += point
                    map.overlays.add(Marker(map).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = dotDrawable(depotColor, sizePx = 40, square = true)
                        title = depot.name
                        snippet = depot.address
                    })
                }
            }

            if (showDrivers) {
                deliveries.forEach { delivery ->
                    val position = delivery.driverPosition ?: return@forEach
                    val point = position.toGeo()
                    allPoints += point
                    map.overlays.add(Marker(map).apply {
                        this.position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        // Solid for live GPS, faded for an estimate from stops/depot.
                        icon = dotDrawable(driverColor, sizePx = 52).apply {
                            if (!delivery.isLive) alpha = 140
                        }
                        title = "${delivery.driverName} · Job #${delivery.job.jobId}"
                        snippet = delivery.positionLabel
                    })
                }
            }

            // Fit everything on first data and whenever the user taps "show everything".
            if (allPoints.isNotEmpty() && (!fitTracker.hasFitted || fitRequest != fitTracker.lastRequest)) {
                fitTracker.hasFitted = true
                fitTracker.lastRequest = fitRequest
                map.post {
                    if (allPoints.size == 1) {
                        map.controller.animateTo(allPoints.first(), 14.0, 500L)
                    } else {
                        map.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(allPoints).increaseByScale(1.3f), true)
                    }
                }
            }

            focusPoint?.let { focus ->
                map.controller.animateTo(focus.toGeo(), 15.0, 600L)
                map.post { onFocusHandled() }
            }

            map.invalidate()
        }
    )
}

private class FitTracker {
    var hasFitted = false
    var lastRequest = 0
}

private fun Coordinates.toGeo() = GeoPoint(latitude, longitude)

private fun dotDrawable(color: Int, sizePx: Int, square: Boolean = false) =
    GradientDrawable().apply {
        shape = if (square) GradientDrawable.RECTANGLE else GradientDrawable.OVAL
        if (square) cornerRadius = sizePx / 4f
        setColor(color)
        setStroke(5, android.graphics.Color.WHITE)
        setSize(sizePx, sizePx)
    }

@Composable
private fun LegendDot(color: Color, square: Boolean = false) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, if (square) RoundedCornerShape(3.dp) else CircleShape)
    )
}

@Composable
private fun ActiveDeliveryCard(
    delivery: ActiveDelivery,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${delivery.driverName} · Job #${delivery.job.jobId}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = delivery.positionLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (delivery.isLive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (delivery.isLive) FontWeight.SemiBold else null
                    )
                }
                StatusBadge(status = delivery.job.status)
            }

            val total = delivery.stops.size
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { delivery.completedStops / total.toFloat() },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${delivery.completedStops} of $total stops delivered",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "No stops added yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
