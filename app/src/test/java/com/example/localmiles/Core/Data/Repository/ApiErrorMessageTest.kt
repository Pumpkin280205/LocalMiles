package com.example.localmiles.Core.Data.Repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.IOException

class ApiErrorMessageTest {

    @Test
    fun `server error JSON is unwrapped instead of shown raw`() {
        val message = httpErrorMessage(400, """{"error":"DepotId is required"}""")
        assertEquals("DepotId is required", message)
    }

    @Test
    fun `duplicate key conflicts name the field to change`() {
        val body = """{"error":"Conflict: Key (EmployeeNumber)=(EMP-0001) already exists."}"""
        assertEquals(
            "That employee number is already in use. Please enter a different one.",
            httpErrorMessage(409, body)
        )
    }

    @Test
    fun `transition conflicts keep the server explanation`() {
        val message = httpErrorMessage(409, """{"error":"Cannot transition job from Pending to Completed"}""")
        assertEquals("Cannot transition job from Pending to Completed", message)
    }

    @Test
    fun `expired session and server faults get friendly text`() {
        assertEquals("Your session has expired. Please log in again.", httpErrorMessage(401, null))
        assertFalse(httpErrorMessage(500, "<html>boom</html>").contains("html"))
    }

    @Test
    fun `network failures are reported as connection problems`() {
        assertEquals(
            "Network error — check your connection and try again",
            friendlyErrorMessage(IOException("timeout"))
        )
    }
}
