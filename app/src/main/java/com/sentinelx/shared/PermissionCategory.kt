package com.sentinelx.shared

enum class PermissionCategory(val emoji: String) {
    CAMERA("📷"),
    MICROPHONE("🎤"),
    LOCATION("📍"),
    CONTACTS("👤"),
    STORAGE("📁"),
    SENSORS("🫀"),
    SMS("💬"),
    PHONE("📞"),
    NETWORK("🌐"),
    OTHER("🔒")
}

fun String.toPermissionCategory(): PermissionCategory {
    return when {
        contains("CAMERA") -> PermissionCategory.CAMERA
        contains("RECORD_AUDIO") -> PermissionCategory.MICROPHONE
        contains("LOCATION") -> PermissionCategory.LOCATION
        contains("CONTACTS") -> PermissionCategory.CONTACTS
        contains("STORAGE") || contains("EXTERNAL") -> PermissionCategory.STORAGE
        contains("SENSORS") || contains("BIOMETRIC") || contains("FINGERPRINT") -> PermissionCategory.SENSORS
        contains("SMS") -> PermissionCategory.SMS
        contains("CALL") || contains("PHONE") -> PermissionCategory.PHONE
        contains("INTERNET") || contains("WIFI") || contains("NETWORK") -> PermissionCategory.NETWORK
        else -> PermissionCategory.OTHER
    }
}