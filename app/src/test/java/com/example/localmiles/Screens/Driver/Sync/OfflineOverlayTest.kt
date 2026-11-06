package com.example.localmiles.Screens.Driver.Sync

import com.example.localmiles.Core.Data.Local.OfflineActionEntity
import com.example.localmiles.Core.Network.Dto.StopResponse
import com.example.localmiles.Core.Network.JobResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineOverlayTest {

    private fun stop(id: Long, status: String) = StopResponse(
        stopId = id, jobId = 1, sequenceNumber = id.toInt(), recipientName = "R",
        address = "A", latitude = 0.0, longitude = 0.0, status = status,
    )

    private fun job(id: Long, status: String) = JobResponse(
        jobId = id, clientId = 1, driverId = 1, vehicleId = null, depotId = 1,
        scheduledDate = "2026-11-06", status = status, priority = "Normal", notes = null,
        estimatedDistanceKm = null, estimatedDurationMinutes = null, createdByUserId = 1, createdAt = "2026-11-06",
    )

    private fun action(type: String, entity: String, id: Long, payload: String, at: Long) =
        OfflineActionEntity(entityType = entity, entityId = id, actionType = type, payload = payload, createdAt = at)

    @Test
    fun `pending stop changes are shown on top of server data`() {
        val stops = listOf(stop(1, "Pending"), stop(2, "Pending"))
        val pending = listOf(action("UpdateStopStatus", "Stop", 1, """{"Status":"Arrived"}""", 1))

        assertEquals(listOf("Arrived", "Pending"), overlayStops(stops, pending).map { it.status })
    }

    @Test
    fun `the latest pending action wins`() {
        val pending = listOf(
            action("UpdateStopStatus", "Stop", 1, """{"Status":"Completed"}""", 2),
            action("UpdateStopStatus", "Stop", 1, """{"Status":"Arrived"}""", 1),
        )

        assertEquals("Completed", overlayStops(listOf(stop(1, "Pending")), pending).single().status)
    }

    @Test
    fun `a pending POD shows the stop as completed`() {
        val pending = listOf(action("SubmitPod", "Stop", 1, """{"RecipientName":"Sam"}""", 1))

        assertEquals("Completed", overlayStops(listOf(stop(1, "Arrived")), pending).single().status)
    }

    @Test
    fun `no pending actions leaves the data untouched`() {
        val stops = listOf(stop(1, "Pending"))
        assertEquals(stops, overlayStops(stops, emptyList()))
    }

    @Test
    fun `job status changes only touch their own job`() {
        val jobs = listOf(job(1, "Assigned"), job(2, "Assigned"))
        val pending = listOf(action("UpdateJobStatus", "Job", 2, """{"Status":"EnRoute"}""", 1))

        assertEquals(listOf("Assigned", "EnRoute"), overlayJobs(jobs, pending).map { it.status })
    }
}
