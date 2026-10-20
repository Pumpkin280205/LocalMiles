package com.example.localmiles.Core.Utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatting {

    private val DISPLAY = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
    private val DISPLAY_DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

    fun parse(iso: String?, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime? {
        if (iso.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(zone).toLocalDateTime() }
            .recoverCatching { Instant.parse(iso).atZone(zone).toLocalDateTime() }
            .recoverCatching { LocalDateTime.parse(iso) }
            .recoverCatching { LocalDate.parse(iso).atStartOfDay() }
            .getOrNull()
    }

    /** "18 Oct 2026, 10:00", or the raw value if it can't be parsed. */
    fun display(iso: String?): String =
        parse(iso)?.format(DISPLAY) ?: iso.orEmpty()

    fun displayDate(date: LocalDate): String = date.format(DISPLAY_DATE)

    /** Local date + time chosen in the pickers -> ISO-8601 with offset for the API. */
    fun toApi(date: LocalDate, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): String =
        date.atTime(hour, minute).atZone(zone).toOffsetDateTime().toString()
}
