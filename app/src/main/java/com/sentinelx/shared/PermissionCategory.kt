package com.sentinelx.shared

enum class PermissionCategory(val displayName: String, val emoji: String) {
    LOCATION("Location", "📍"),
    CAMERA("Camera", "📷"),
    MICROPHONE("Microphone", "🎤"),
    CONTACTS("Contacts", "👤"),
    STORAGE("Storage", "💾"),
    NETWORK("Network & Connectivity", "🌐"),
    SENSORS("Body Sensors", "❤️"),
    SYSTEM("System Level", "⚙️"),
    UNKNOWN("Other", "❓")
}

fun String.toPermissionCategory(): PermissionCategory {
    return when {
        contains("LOCATION") || contains("location") -> PermissionCategory.LOCATION
        contains("CAMERA") || contains("camera") -> PermissionCategory.CAMERA
        contains("RECORD_AUDIO") || contains("record_audio") -> PermissionCategory.MICROPHONE
        contains("CONTACTS") || contains("contacts") -> PermissionCategory.CONTACTS
        contains("STORAGE") || contains("storage") || contains("MEDIA") -> PermissionCategory.STORAGE
        contains("INTERNET") || contains("WIFI") || contains("BLUETOOTH") || contains("NFC") -> PermissionCategory.NETWORK
        contains("SENSOR") || contains("BIOMETRIC") || contains("FINGERPRINT") || contains("ACTIVITY") -> PermissionCategory.SENSORS
        contains("INSTALL") || contains("PHONE_STATE") || contains("CALL") || contains("SMS") -> PermissionCategory.SYSTEM
        else -> PermissionCategory.UNKNOWN
    }
}

fun List<String>.groupByCategory(): Map<PermissionCategory, List<String>> {
    return this.groupBy { it.toPermissionCategory() }
        .toSortedMap(compareBy { it.ordinal })
}