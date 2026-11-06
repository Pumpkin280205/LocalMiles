package com.example.localmiles.Screens.Dispatcher

import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.HistoryRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class HistoryFilterTest {

    private val now = LocalDateTime.of(2026, 10, 18, 12, 0)

    private fun job(
        id: Long,
        status: String,
        date: String,
        driverId: Long? = 1,
        clientId: Long = 1,
        priority: String = "Normal",
        notes: String? = null,
    ) = JobResponse(id, clientId, driverId, null, 1, date, status, priority, notes, null, null, 1, date)

    private val jobs = listOf(
        job(1, "Completed", "2026-10-17T09:00:00"),
        job(2, "Cancelled", "2026-10-10T09:00:00", driverId = 2),
        job(3, "Completed", "2026-08-01T09:00:00", clientId = 2, priority = "Urgent"),
        job(4, "EnRoute", "2026-10-18T09:00:00"),
        job(5, "Completed", "2026-10-15T09:00:00", notes = "Fragile glassware"),
    )

    @Test
    fun `only closed jobs appear, newest first`() {
        val result = filterHistory(jobs, HistoryFilter(range = HistoryRange.ALL_TIME), now)
        assertEquals(listOf(1L, 5L, 2L, 3L), result.map { it.jobId })
    }

    @Test
    fun `date range excludes older jobs`() {
        val result = filterHistory(jobs, HistoryFilter(range = HistoryRange.LAST_7_DAYS), now)
        assertEquals(listOf(1L, 5L), result.map { it.jobId })
    }

    @Test
    fun `status, driver, client and priority filters combine`() {
        val all = HistoryFilter(range = HistoryRange.ALL_TIME)
        assertEquals(listOf(2L), filterHistory(jobs, all.copy(status = "Cancelled"), now).map { it.jobId })
        assertEquals(listOf(2L), filterHistory(jobs, all.copy(driverId = 2), now).map { it.jobId })
        assertEquals(listOf(3L), filterHistory(jobs, all.copy(clientId = 2, priority = "Urgent"), now).map { it.jobId })
    }

    @Test
    fun `search matches job number, notes and resolved names`() {
        val all = HistoryFilter(range = HistoryRange.ALL_TIME)
        assertEquals(listOf(3L), filterHistory(jobs, all.copy(query = "#3"), now).map { it.jobId })
        assertEquals(listOf(5L), filterHistory(jobs, all.copy(query = "glass"), now).map { it.jobId })
        val byName = filterHistory(jobs, all.copy(query = "acme"), now) { if (it.clientId == 2L) "Acme Ltd" else "" }
        assertEquals(listOf(3L), byName.map { it.jobId })
    }

    @Test
    fun `oldest-first sort reverses the order`() {
        val result = filterHistory(jobs, HistoryFilter(range = HistoryRange.ALL_TIME, sort = HistorySort.OLDEST), now)
        assertEquals(listOf(3L, 2L, 5L, 1L), result.map { it.jobId })
    }
}
