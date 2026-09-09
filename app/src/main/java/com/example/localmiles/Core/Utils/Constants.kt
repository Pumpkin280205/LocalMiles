package com.example.localmiles.Core.Utils

/**
 * Role strings exactly as the API returns them in the login response
 * (`user.role`). The backend defines admin/driver in middleware/auth.ts and
 * creates new SSO users as "driver"; "client" comes from seeded rows.
 */
object Role {
    const val ADMIN = "admin"      // the "dispatcher" persona in the UI
    const val DRIVER = "driver"
    const val CLIENT = "client"
}
