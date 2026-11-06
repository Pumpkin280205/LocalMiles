package com.example.localmiles.Screens.Dispatcher

import com.example.localmiles.Core.Network.Dto.DriverResponse

/** Field-level problems with the "Make driver" form, keyed for the UI. */
data class DriverFormErrors(
    val depot: String? = null,
    val employeeNumber: String? = null,
    val phoneNumber: String? = null,
    val licenceNumber: String? = null,
) {
    val hasErrors: Boolean
        get() = listOfNotNull(depot, employeeNumber, phoneNumber, licenceNumber).isNotEmpty()
}

/**
 * Checks the Driver record before it's sent, so the dispatcher sees "employee
 * number already used by another driver" next to the field instead of a raw
 * server conflict. 
 */
object DriverFormValidator {

    // SA numbers: 0XXXXXXXXX or +27XXXXXXXXX, spaces/dashes ignored.
    private val PHONE = Regex("""^(0\d{9}|\+27\d{9})$""")
    private val LICENCE = Regex("""^[A-Z0-9]{4,20}$""")

    fun normalisePhone(raw: String): String = raw.replace(Regex("""[\s\-()]"""), "")

    fun validate(
        userId: Long,
        depotId: Long?,
        employeeNumber: String,
        phoneNumber: String,
        licenceNumber: String,
        existingDrivers: List<DriverResponse>,
    ): DriverFormErrors {
        val others = existingDrivers.filter { it.userId != userId }
        val employee = employeeNumber.trim()
        val licence = licenceNumber.trim().uppercase()

        return DriverFormErrors(
            depot = if (depotId == null) "Choose the depot this driver works from" else null,
            employeeNumber = when {
                employee.isEmpty() -> "Employee number is required"
                others.any { it.employeeNumber.equals(employee, ignoreCase = true) } ->
                    "Already used by another driver"
                else -> null
            },
            phoneNumber = when {
                phoneNumber.isBlank() -> "Phone number is required"
                !PHONE.matches(normalisePhone(phoneNumber)) -> "Use a 10-digit number like 082 123 4567"
                else -> null
            },
            licenceNumber = when {
                licence.isEmpty() -> "Licence number is required"
                !LICENCE.matches(licence) -> "Letters and numbers only (4–20 characters)"
                others.any { it.licenceNumber.equals(licence, ignoreCase = true) } ->
                    "Already registered to another driver"
                else -> null
            },
        )
    }

    /** Next free "EMP-0001"-style number, so the admin doesn't have to invent one. */
    fun suggestEmployeeNumber(existingDrivers: List<DriverResponse>): String {
        val highest = existingDrivers
            .mapNotNull { Regex("""(\d+)$""").find(it.employeeNumber)?.value?.toIntOrNull() }
            .maxOrNull() ?: 0
        var candidate = highest + 1
        while (existingDrivers.any { it.employeeNumber.equals("EMP-%04d".format(candidate), ignoreCase = true) }) {
            candidate++
        }
        return "EMP-%04d".format(candidate)
    }
}
