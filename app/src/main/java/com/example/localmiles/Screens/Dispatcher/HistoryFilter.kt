package com.example.localmiles.Screens.Dispatcher

import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.DateFormatting
import com.example.localmiles.Core.Utils.HistoryRange
import com.example.localmiles.Core.Utils.JobStatus
import java.time.LocalDateTime

enum class HistorySort(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
}

/** Everything the Delivery History screen can filter by. */
data class HistoryFilter(
    val query: String = "",
    // null = Completed and Cancelled.
    val status: String? = null,
    val range: HistoryRange = HistoryRange.LAST_30_DAYS,
    val driverId: Long? = null,
    val clientId: Long? = null,
    val priority: String? = null,
    val sort: HistorySort = HistorySort.NEWEST,
) {
    val activeFilterCount: Int
        get() = listOfNotNull(status, driverId, clientId, priority).size +
            (if (query.isNotBlank()) 1 else 0)
}

/**
 * Pure filtering for Delivery History, kept out of the composable so it can be
 * unit tested.
 */
fun filterHistory(
    jobs: List<JobResponse>,
    filter: HistoryFilter,
    now: LocalDateTime = LocalDateTime.now(),
    names: (JobResponse) -> String = { "" },
): List<JobResponse> {
    val cutoff = filter.range.days?.let { now.minusDays(it) }
    val query = filter.query.trim()

    return jobs
        .filter { JobStatus.isClosed(it.status) }
        .filter { filter.status == null || it.status == filter.status }
        .filter { filter.driverId == null || it.driverId == filter.driverId }
        .filter { filter.clientId == null || it.clientId == filter.clientId }
        .filter { filter.priority == null || it.priority == filter.priority }
        .filter { job ->
            if (cutoff == null) return@filter true
            val date = DateFormatting.parse(job.scheduledDate) ?: return@filter true
            !date.isBefore(cutoff)
        }
        .filter { job ->
            query.isEmpty() ||
                job.jobId.toString() == query.removePrefix("#") ||
                job.notes.orEmpty().contains(query, ignoreCase = true) ||
                names(job).contains(query, ignoreCase = true)
        }
        .let { filtered ->
            val byDate = filtered.sortedBy { DateFormatting.parse(it.scheduledDate) ?: LocalDateTime.MIN }
            if (filter.sort == HistorySort.NEWEST) byDate.reversed() else byDate
        }
}
