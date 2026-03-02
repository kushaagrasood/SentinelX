package com.sentinelx.shared

data class MonitorEvent(
    val timestamp: Long,
    val packageName: String,
    val appName: String,
    val permissionTriggered: String,
    val wasBackground: Boolean,
    val durationMs: Long,
    val category: PermissionCategory
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