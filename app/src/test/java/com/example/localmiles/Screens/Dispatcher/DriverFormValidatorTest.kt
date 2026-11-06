package com.example.localmiles.Screens.Dispatcher

import com.example.localmiles.Core.Network.Dto.DriverResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverFormValidatorTest {

    private val existing = listOf(
        DriverResponse(1, userId = 10, depotId = 1, employeeNumber = "EMP-0001", phoneNumber = "0821111111",
            licenceNumber = "ABC12345", isAvailable = true, isActive = true),
        DriverResponse(2, userId = 11, depotId = 1, employeeNumber = "EMP-0007", phoneNumber = "0822222222",
            licenceNumber = "XYZ98765", isAvailable = true, isActive = false),
    )

    @Test
    fun `valid details pass`() {
        val errors = DriverFormValidator.validate(20, 1, "EMP-0008", "082 123 4567", "new12345", existing)
        assertFalse(errors.hasErrors)
    }

    @Test
    fun `duplicate employee and licence numbers are caught before the API call`() {
        val errors = DriverFormValidator.validate(20, 1, "emp-0001", "0821234567", "xyz98765", existing)
        assertNotNull(errors.employeeNumber)
        assertNotNull(errors.licenceNumber)
    }

    @Test
    fun `a returning driver may keep their own old numbers`() {
        val errors = DriverFormValidator.validate(11, 1, "EMP-0007", "0822222222", "XYZ98765", existing)
        assertFalse(errors.hasErrors)
    }

    @Test
    fun `phone numbers must be South African format`() {
        assertNull(DriverFormValidator.validate(20, 1, "E9", "+27821234567", "AB1234", existing).phoneNumber)
        assertNotNull(DriverFormValidator.validate(20, 1, "E9", "12345", "AB1234", existing).phoneNumber)
    }

    @Test
    fun `missing depot and blank fields are reported`() {
        val errors = DriverFormValidator.validate(20, null, "", "", "", existing)
        assertTrue(errors.hasErrors)
        assertNotNull(errors.depot)
        assertNotNull(errors.employeeNumber)
        assertNotNull(errors.phoneNumber)
        assertNotNull(errors.licenceNumber)
    }

    @Test
    fun `suggested employee number is the next free one`() {
        assertEquals("EMP-0008", DriverFormValidator.suggestEmployeeNumber(existing))
        assertEquals("EMP-0001", DriverFormValidator.suggestEmployeeNumber(emptyList()))
    }
}
