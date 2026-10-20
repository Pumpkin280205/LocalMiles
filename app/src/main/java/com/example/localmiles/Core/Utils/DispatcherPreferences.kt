package com.example.localmiles.Core.Utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How far back Delivery History looks by default. */
enum class HistoryRange(val label: String, val days: Long?) {
    LAST_7_DAYS("7 days", 7),
    LAST_30_DAYS("30 days", 30),
    LAST_90_DAYS("90 days", 90),
    ALL_TIME("All time", null),
}

data class DispatcherSettings(
    // Pre-selected depot (and its drivers/vehicles) on Create Job.
    val defaultDepotId: Long? = null,
    val defaultPriority: String = JobPriority.NORMAL,
    // Live Map polls the API on this interval while it's open.
    val mapAutoRefresh: Boolean = true,
    val mapRefreshSeconds: Int = 30,
    val historyRange: HistoryRange = HistoryRange.LAST_30_DAYS,
    // After creating a job, jump straight into adding its delivery stops.
    val promptForStopsAfterCreate: Boolean = true,
    // Ask before a status change (dispatch / complete / cancel) is sent.
    val confirmStatusChanges: Boolean = true,
)

/**
 * Admin-only operational preferences set on the Dispatcher Settings screen and
 * read by Create Job, Jobs, Live Map and Delivery History. 
 */
object DispatcherPreferences {

    val MAP_REFRESH_OPTIONS = listOf(15, 30, 60)

    private var prefs: SharedPreferences? = null

    private val _settings = MutableStateFlow(DispatcherSettings())
    val settings: StateFlow<DispatcherSettings> = _settings.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return

        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p

        _settings.value = DispatcherSettings(
            defaultDepotId = p.getLong(KEY_DEFAULT_DEPOT, -1L).takeIf { it > 0 },
            defaultPriority = p.getString(KEY_DEFAULT_PRIORITY, null)
                ?.takeIf { it in JobPriority.ALL } ?: JobPriority.NORMAL,
            mapAutoRefresh = p.getBoolean(KEY_MAP_AUTO_REFRESH, true),
            mapRefreshSeconds = p.getInt(KEY_MAP_REFRESH_SECONDS, 30),
            historyRange = runCatching {
                HistoryRange.valueOf(p.getString(KEY_HISTORY_RANGE, null) ?: "")
            }.getOrDefault(HistoryRange.LAST_30_DAYS),
            promptForStopsAfterCreate = p.getBoolean(KEY_PROMPT_STOPS, true),
            confirmStatusChanges = p.getBoolean(KEY_CONFIRM_STATUS, true),
        )
    }

    fun update(transform: (DispatcherSettings) -> DispatcherSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated

        prefs?.edit()
            ?.putLong(KEY_DEFAULT_DEPOT, updated.defaultDepotId ?: -1L)
            ?.putString(KEY_DEFAULT_PRIORITY, updated.defaultPriority)
            ?.putBoolean(KEY_MAP_AUTO_REFRESH, updated.mapAutoRefresh)
            ?.putInt(KEY_MAP_REFRESH_SECONDS, updated.mapRefreshSeconds)
            ?.putString(KEY_HISTORY_RANGE, updated.historyRange.name)
            ?.putBoolean(KEY_PROMPT_STOPS, updated.promptForStopsAfterCreate)
            ?.putBoolean(KEY_CONFIRM_STATUS, updated.confirmStatusChanges)
            ?.apply()
    }

    fun reset() = update { DispatcherSettings() }

    private const val PREFS_NAME = "localmiles_dispatcher_prefs"
    private const val KEY_DEFAULT_DEPOT = "default_depot_id"
    private const val KEY_DEFAULT_PRIORITY = "default_priority"
    private const val KEY_MAP_AUTO_REFRESH = "map_auto_refresh"
    private const val KEY_MAP_REFRESH_SECONDS = "map_refresh_seconds"
    private const val KEY_HISTORY_RANGE = "history_range"
    private const val KEY_PROMPT_STOPS = "prompt_stops_after_create"
    private const val KEY_CONFIRM_STATUS = "confirm_status_changes"
}
