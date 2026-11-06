package com.example.localmiles.Core.Utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobStatusTest {

    @Test
    fun `next statuses mirror the API transition table`() {
        assertEquals(listOf("Assigned", "Cancelled"), JobStatus.nextStatuses("Pending"))
        assertEquals(listOf("EnRoute", "Pending", "Cancelled"), JobStatus.nextStatuses("Assigned"))
        assertEquals(listOf("Completed", "Cancelled"), JobStatus.nextStatuses("EnRoute"))
        assertTrue(JobStatus.nextStatuses("Completed").isEmpty())
        assertTrue(JobStatus.nextStatuses("Cancelled").isEmpty())
    }

    @Test
    fun `jumping straight from Pending to Completed is not allowed`() {
        assertFalse(JobStatus.canTransition("Pending", "Completed"))
        assertFalse(JobStatus.canTransition("Pending", "EnRoute"))
    }

    @Test
    fun `same status is always a valid no-op`() {
        JobStatus.ALL.forEach { assertTrue(JobStatus.canTransition(it, it)) }
    }

    @Test
    fun `closed statuses and labels`() {
        assertTrue(JobStatus.isClosed("Completed"))
        assertTrue(JobStatus.isClosed("Cancelled"))
        assertFalse(JobStatus.isClosed("EnRoute"))
        assertEquals("En Route", JobStatus.label("EnRoute"))
    }
}
