package com.example.localmiles.Screens.Dispatcher

import com.example.localmiles.Core.Network.Dto.DepotResponse
import com.example.localmiles.Core.Network.Dto.StopResponse
import com.example.localmiles.Core.Network.JobResponse
import com.example.localmiles.Core.Utils.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMapModelTest {

    private val depot = DepotResponse(1, "Durban Depot", "1 Point Rd", -29.86, 31.03, null, true)

    private fun job(id: Long, status: String) =
        JobResponse(id, 1, 1, null, 1, "2026-10-18T09:00:00Z", status, "Normal", null, null, null, 1, "2026-10-18")

    private fun stop(seq: Int, status: String, lat: Double = -29.80 - seq / 100.0) =
        StopResponse(seq.toLong(), 1, seq, "Recipient $seq", "$seq Smith St", lat, 31.0, status = status)

    @Test
    fun `an assigned job waits at its depot`() {
        val (position, label) = estimateDriverPosition(job(1, "Assigned"), emptyList(), depot)
        assertEquals(Coordinates(-29.86, 31.03), position)
        assertTrue(label.contains("Durban Depot"))
    }

    @Test
    fun `an en route driver is at the last stop they reached`() {
        val stops = listOf(stop(1, "Completed"), stop(2, "Arrived"), stop(3, "Pending"))
        val (position, label) = estimateDriverPosition(job(1, "EnRoute"), stops, depot)
        assertEquals(stops[1].latitude, position!!.latitude, 0.0)
        assertTrue(label.startsWith("At stop 2"))
    }

    @Test
    fun `stops saved at 0,0 are never plotted`() {
        assertFalse(Coordinates(0.0, 0.0).isPlottable())
        assertTrue(Coordinates(-29.86, 31.03).isPlottable())

        val stops = listOf(stop(1, "Completed", lat = 0.0).copy(longitude = 0.0))
        val (position, _) = estimateDriverPosition(job(1, "EnRoute"), stops, depot)
        assertEquals(Coordinates(-29.86, 31.03), position)
    }

    @Test
    fun `active deliveries list en route jobs first`() {
        val state = DispatcherUiState(
            jobs = listOf(job(1, "Assigned"), job(2, "EnRoute"), job(3, "Completed")),
            depots = listOf(depot),
        )
        assertEquals(listOf(2L, 1L), buildActiveDeliveries(state).map { it.job.jobId })
    }
}
