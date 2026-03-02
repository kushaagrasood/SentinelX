package com.sentinelx.shared

data class AnomalyEvent(
    val timestamp: Long,
    val type: AnomalyType,
    val description: String
)

enum class AnomalyType {
    CAMERA_UNUSUAL_HOUR,        // camera used between midnight and 6am
    MIC_BACKGROUND_SPIKE,       // mic used while app was in background
    LOCATION_EXCESSIVE,         // location accessed more than 20 times today
    NETWORK_SPIKE,              // sudden large data transmission
    PERMISSION_ESCALATION       // multiple new permissions granted recently
}