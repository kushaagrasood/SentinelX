package com.sentinelx.shared

data class MonitorEvent(
    val timestamp: Long,
    val packageName: String,
    val appName: String,
    val permissionTriggered: String,    // e.g. "android.permission.CAMERA"
    val wasBackground: Boolean,         // true if app was not in foreground
    val durationMs: Long,               // how long the permission was active
    val category: PermissionCategory    // auto-derived from permissionTriggered
) {
    companion object {
        fun create(
            packageName: String,
            appName: String,
            permissionTriggered: String,
            wasBackground: Boolean,
            durationMs: Long = 0L
        ): MonitorEvent {
            return MonitorEvent(
                timestamp = System.currentTimeMillis(),
                packageName = packageName,
                appName = appName,
                permissionTriggered = permissionTriggered,
                wasBackground = wasBackground,
                durationMs = durationMs,
                category = permissionTriggered.toPermissionCategory()
            )
        }
    }
}