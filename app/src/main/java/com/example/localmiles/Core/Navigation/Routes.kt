package com.example.localmiles.Core.Navigation

import com.example.localmiles.Core.Utils.Role

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"

    const val DISPATCHER_DASHBOARD = "dispatcher/dashboard"
    const val DRIVER_DASHBOARD = "driver/dashboard"
    const val CLIENT_DASHBOARD = "client/dashboard"
}

/** Single source of truth for which home screen a role lands on after login. */
fun homeRouteForRole(role: String?): String = when (role) {
    Role.ADMIN -> Routes.DISPATCHER_DASHBOARD
    Role.DRIVER -> Routes.DRIVER_DASHBOARD
    Role.CLIENT -> Routes.CLIENT_DASHBOARD
    else -> Routes.LOGIN            // null / unknown role -> force re-login
}
