package com.example.localmiles.Core.Utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DateFormattingTest {

    private val johannesburg = ZoneId.of("Africa/Johannesburg")

    @Test
    fun `UTC timestamps from the API are shown in local time`() {
        assertEquals(
            LocalDateTime.of(2026, 9, 20, 10, 0),
            DateFormatting.parse("2026-09-20T08:00:00.000Z", johannesburg)
        )
    }

    @Test
    fun `offset-less and date-only values still parse`() {
        assertEquals(LocalDateTime.of(2026, 9, 20, 10, 0), DateFormatting.parse("2026-09-20T10:00:00"))
        assertEquals(LocalDateTime.of(2026, 9, 20, 0, 0), DateFormatting.parse("2026-09-20"))
        assertNull(DateFormatting.parse("not a date"))
    }

    @Test
    fun `picker values become an ISO string with offset`() {
        assertEquals(
            "2026-10-19T09:30+02:00",
            DateFormatting.toApi(LocalDate.of(2026, 10, 19), 9, 30, johannesburg)
        )
    }
}
