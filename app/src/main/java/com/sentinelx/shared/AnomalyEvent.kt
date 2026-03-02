package com.sentinelx.shared

enum class AnomalyType {
    CAMERA_UNUSUAL_HOUR,
    MIC_BACKGROUND_SPIKE,
    LOCATION_EXCESSIVE,
    NETWORK_SPIKE,
    PERMISSION_ESCALATION
}

data class AnomalyEvent(
    val timestamp: Long,
    val type: AnomalyType,
    val description: String,
    val packageName: String = ""
)