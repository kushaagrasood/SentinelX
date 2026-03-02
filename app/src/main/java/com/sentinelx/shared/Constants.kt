package com.sentinelx.shared

object Constants {

    // Risk levels
    const val RISK_CRITICAL = "CRITICAL"
    const val RISK_HIGH = "HIGH"
    const val RISK_MEDIUM = "MEDIUM"
    const val RISK_LOW = "LOW"

    // Risk thresholds
    const val RISK_CRITICAL_THRESHOLD = 80
    const val RISK_HIGH_THRESHOLD = 60
    const val RISK_MEDIUM_THRESHOLD = 25

    // Permission weights — higher = more sensitive
    val PERMISSION_WEIGHTS = mapOf(
        // Core surveillance
        "android.permission.CAMERA" to 30,
        "android.permission.RECORD_AUDIO" to 30,
        "android.permission.ACCESS_FINE_LOCATION" to 25,
        "android.permission.ACCESS_BACKGROUND_LOCATION" to 30,  // background is worse

        // Communication
        "android.permission.READ_SMS" to 20,
        "android.permission.SEND_SMS" to 20,
        "android.permission.READ_CALL_LOG" to 20,
        "android.permission.WRITE_CALL_LOG" to 20,
        "android.permission.PROCESS_OUTGOING_CALLS" to 15,
        "android.permission.CALL_PHONE" to 15,

        // Personal data
        "android.permission.READ_CONTACTS" to 15,
        "android.permission.WRITE_CONTACTS" to 15,
        "android.permission.GET_ACCOUNTS" to 15,
        "android.permission.READ_CALENDAR" to 10,
        "android.permission.WRITE_CALENDAR" to 10,

        // Device sensors
        "android.permission.BODY_SENSORS" to 20,
        "android.permission.BODY_SENSORS_BACKGROUND" to 25,
        "android.permission.ACTIVITY_RECOGNITION" to 15,

        // Storage
        "android.permission.READ_EXTERNAL_STORAGE" to 10,
        "android.permission.WRITE_EXTERNAL_STORAGE" to 10,
        "android.permission.MANAGE_EXTERNAL_STORAGE" to 20,  // full storage access

        // Network & connectivity
        "android.permission.INTERNET" to 10,
        "android.permission.ACCESS_WIFI_STATE" to 8,
        "android.permission.CHANGE_WIFI_STATE" to 10,
        "android.permission.BLUETOOTH_SCAN" to 12,
        "android.permission.BLUETOOTH_CONNECT" to 12,
        "android.permission.NFC" to 10,
        "android.permission.ACCESS_COARSE_LOCATION" to 10,

        // System level
        "android.permission.READ_PHONE_STATE" to 15,
        "android.permission.READ_PHONE_NUMBERS" to 15,
        "android.permission.USE_BIOMETRIC" to 15,
        "android.permission.USE_FINGERPRINT" to 15,
        "android.permission.REQUEST_INSTALL_PACKAGES" to 20  // can install apps silently
    )

    val SENSITIVE_PERMISSIONS = PERMISSION_WEIGHTS.keys.toSet()

    // AppOps tracking strings
    val APPOPS_MAP = mapOf(
        "android.permission.CAMERA" to "android:camera",
        "android.permission.RECORD_AUDIO" to "android:record_audio",
        "android.permission.ACCESS_FINE_LOCATION" to "android:fine_location",
        "android.permission.ACCESS_COARSE_LOCATION" to "android:coarse_location",
        "android.permission.READ_CONTACTS" to "android:read_contacts",
        "android.permission.READ_SMS" to "android:read_sms",
        "android.permission.READ_CALL_LOG" to "android:read_call_log",
        "android.permission.BODY_SENSORS" to "android:body_sensors",
        "android.permission.ACTIVITY_RECOGNITION" to "android:activity_recognition"
    )

    // Notification channels
    const val CHANNEL_ID_MONITOR = "sentinelx_monitor"
    const val CHANNEL_ID_ALERT = "sentinelx_alert"
    const val CHANNEL_ID_ANOMALY = "sentinelx_anomaly"
    const val CHANNEL_NAME_MONITOR = "Monitor Status"
    const val CHANNEL_NAME_ALERT = "Privacy Alerts"
    const val CHANNEL_NAME_ANOMALY = "Anomaly Detections"

    // Anomaly detection thresholds
    const val ANOMALY_HOUR_START = 0    // midnight
    const val ANOMALY_HOUR_END = 6      // 6am — camera/mic use in this window is flagged
    const val ANOMALY_LOCATION_LIMIT = 20  // accesses per day before flagged
    const val ANOMALY_NETWORK_SPIKE_BYTES = 10_000_000L  // 10MB sudden spike

    // Auto-revoke suggestion: high risk + not used in this many days
    const val AUTO_REVOKE_UNUSED_DAYS = 7L

    // Risk history: keep last N snapshots per app
    const val RISK_HISTORY_MAX = 30

    // Permission change window (ms) — 7 days
    const val PERMISSION_CHANGE_WINDOW_MS = 7 * 24 * 60 * 60 * 1000L
}