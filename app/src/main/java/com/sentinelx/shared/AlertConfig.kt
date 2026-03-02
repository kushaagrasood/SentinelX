package com.sentinelx.shared

data class AlertConfig(
    val enabledPermissions: Set<String>,    // which permissions trigger alerts
    val quietHoursEnabled: Boolean,
    val quietHourStart: Int,                // 0–23 hour
    val quietHourEnd: Int,                  // 0–23 hour
    val whitelistedPackages: Set<String>,   // never alert for these apps
    val alertOnBackground: Boolean,         // only alert if app is in background
    val vibrate: Boolean,
    val sound: Boolean,
    val showOverlay: Boolean                // SYSTEM_ALERT_WINDOW overlay
) {
    companion object {
        // Sensible defaults
        fun default(): AlertConfig = AlertConfig(
            enabledPermissions = setOf(
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_BACKGROUND_LOCATION"
            ),
            quietHoursEnabled = false,
            quietHourStart = 23,
            quietHourEnd = 7,
            whitelistedPackages = emptySet(),
            alertOnBackground = true,
            vibrate = true,
            sound = true,
            showOverlay = false
        )
    }

    fun isQuietHour(): Boolean {
        if (!quietHoursEnabled) return false
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return if (quietHourStart > quietHourEnd) {
            // Wraps midnight e.g. 23:00 to 07:00
            hour !in quietHourEnd..<quietHourStart
        } else {
            hour in quietHourStart until quietHourEnd
        }
    }

    fun shouldAlert(packageName: String, permission: String, isBackground: Boolean): Boolean {
        if (isQuietHour()) return false
        if (packageName in whitelistedPackages) return false
        if (permission !in enabledPermissions) return false
        if (alertOnBackground && !isBackground) return false
        return true
    }

    fun withWhitelisted(packageName: String): AlertConfig =
        copy(whitelistedPackages = whitelistedPackages + packageName)

    fun withoutWhitelisted(packageName: String): AlertConfig =
        copy(whitelistedPackages = whitelistedPackages - packageName)
}