package com.sentinelx.shared

object Constants {

    const val RISK_HIGH = "HIGH"
    const val RISK_MEDIUM = "MEDIUM"
    const val RISK_LOW = "LOW"

    const val RISK_HIGH_THRESHOLD = 60
    const val RISK_MEDIUM_THRESHOLD = 25

    val PERMISSION_WEIGHTS = mapOf(
        "android.permission.CAMERA" to 30,
        "android.permission.RECORD_AUDIO" to 30,
        "android.permission.ACCESS_FINE_LOCATION" to 25,
        "android.permission.READ_SMS" to 20,
        "android.permission.READ_CALL_LOG" to 20,
        "android.permission.BODY_SENSORS" to 20,
        "android.permission.READ_CONTACTS" to 15,
        "android.permission.PROCESS_OUTGOING_CALLS" to 15,
        "android.permission.ACCESS_COARSE_LOCATION" to 10,
        "android.permission.READ_EXTERNAL_STORAGE" to 10
    )

    val SENSITIVE_PERMISSIONS = PERMISSION_WEIGHTS.keys.toSet()

    const val CHANNEL_ID_MONITOR = "sentinelx_monitor"
    const val CHANNEL_ID_ALERT = "sentinelx_alert"

    const val CHANNEL_NAME_MONITOR = "Monitor Status"
    const val CHANNEL_NAME_ALERT = "Privacy Alerts"

    val APPOPS_MAP = mapOf(
        "android.permission.CAMERA" to "android:camera",
        "android.permission.RECORD_AUDIO" to "android:record_audio",
        "android.permission.ACCESS_FINE_LOCATION" to "android:fine_location"
    )
}