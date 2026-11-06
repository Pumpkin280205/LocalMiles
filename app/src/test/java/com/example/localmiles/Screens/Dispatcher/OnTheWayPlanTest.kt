package com.example.localmiles.Screens.Dispatcher

import com.example.localmiles.Core.Network.Dto.StopResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnTheWayPlanTest {

    private fun stop(id: Long, sequence: Int) = StopResponse(
        stopId = id, jobId = 1, sequenceNumber = sequence, recipientName = "R$id",
        address = "Address $id", latitude = 0.0, longitude = 0.0, status = "Pending",
    )

    @Test
    fun `a job with only its delivery address gets the new stop before it`() {
        val plan = planOnTheWayStop(listOf(stop(1, 1)))

        assertEquals(1L, plan.destination?.stopId)
        assertEquals(1, plan.newSequence)   // takes the destination's place, the destination moves to 2
    }

    @Test
    fun `the destination is the stop with the highest order, whatever the list order`() {
        val plan = planOnTheWayStop(listOf(stop(3, 3), stop(1, 1), stop(2, 2)))

        assertEquals(3L, plan.destination?.stopId)
        assertEquals(3, plan.newSequence)
    }

    @Test
    fun `a job with no stops yet starts at 1`() {
        val plan = planOnTheWayStop(emptyList())

        assertNull(plan.destination)
        assertEquals(1, plan.newSequence)
    }
}
