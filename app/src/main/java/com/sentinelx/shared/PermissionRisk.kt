package com.sentinelx.shared

enum class DangerLevel(val displayName: String) {
    CRITICAL("Critical"),
    DANGEROUS("Dangerous"),
    MODERATE("Moderate"),
    NORMAL("Normal")
}

data class PermissionRisk(
    val permissionString: String,
    val displayName: String,
    val category: PermissionCategory,
    val weight: Int,
    val dangerLevel: DangerLevel,
    val explanation: String,            // why this is risky
    val realWorldExample: String,       // e.g. "Used by spyware to record conversations"
    val isRuntime: Boolean,             // true = user must explicitly grant
    val canBeBackground: Boolean        // true = can activate without user knowing
) {
    companion object {
        val ALL: Map<String, PermissionRisk> = mapOf(
            "android.permission.CAMERA" to PermissionRisk(
                permissionString = "android.permission.CAMERA",
                displayName = "Camera",
                category = PermissionCategory.CAMERA,
                weight = 30,
                dangerLevel = DangerLevel.CRITICAL,
                explanation = "Allows the app to take photos and record video at any time.",
                realWorldExample = "Stalkerware uses this to silently photograph users.",
                isRuntime = true,
                canBeBackground = true
            ),
            "android.permission.RECORD_AUDIO" to PermissionRisk(
                permissionString = "android.permission.RECORD_AUDIO",
                displayName = "Microphone",
                category = PermissionCategory.MICROPHONE,
                weight = 30,
                dangerLevel = DangerLevel.CRITICAL,
                explanation = "Allows the app to record audio through the microphone.",
                realWorldExample = "Malicious apps use this to eavesdrop on conversations.",
                isRuntime = true,
                canBeBackground = true
            ),
            "android.permission.ACCESS_FINE_LOCATION" to PermissionRisk(
                permissionString = "android.permission.ACCESS_FINE_LOCATION",
                displayName = "Precise Location",
                category = PermissionCategory.LOCATION,
                weight = 25,
                dangerLevel = DangerLevel.CRITICAL,
                explanation = "Tracks your exact GPS location, accurate to a few meters.",
                realWorldExample = "Can be used to track daily routines and home address.",
                isRuntime = true,
                canBeBackground = true
            ),
            "android.permission.ACCESS_BACKGROUND_LOCATION" to PermissionRisk(
                permissionString = "android.permission.ACCESS_BACKGROUND_LOCATION",
                displayName = "Background Location",
                category = PermissionCategory.LOCATION,
                weight = 30,
                dangerLevel = DangerLevel.CRITICAL,
                explanation = "Tracks your location even when the app is not in use.",
                realWorldExample = "Continuously monitors movement without user awareness.",
                isRuntime = true,
                canBeBackground = true
            ),
            "android.permission.READ_SMS" to PermissionRisk(
                permissionString = "android.permission.READ_SMS",
                displayName = "Read SMS",
                category = PermissionCategory.SYSTEM,
                weight = 20,
                dangerLevel = DangerLevel.DANGEROUS,
                explanation = "Reads all your text messages including OTPs and private chats.",
                realWorldExample = "Used to steal two-factor authentication codes.",
                isRuntime = true,
                canBeBackground = false
            ),
            "android.permission.READ_CONTACTS" to PermissionRisk(
                permissionString = "android.permission.READ_CONTACTS",
                displayName = "Read Contacts",
                category = PermissionCategory.CONTACTS,
                weight = 15,
                dangerLevel = DangerLevel.DANGEROUS,
                explanation = "Accesses your entire contacts list including names, numbers, emails.",
                realWorldExample = "Sold to data brokers for targeted advertising.",
                isRuntime = true,
                canBeBackground = false
            ),
            "android.permission.BODY_SENSORS" to PermissionRisk(
                permissionString = "android.permission.BODY_SENSORS",
                displayName = "Body Sensors",
                category = PermissionCategory.SENSORS,
                weight = 20,
                dangerLevel = DangerLevel.DANGEROUS,
                explanation = "Reads health data from wearables like heart rate and step count.",
                realWorldExample = "Can infer health conditions and sell to insurance companies.",
                isRuntime = true,
                canBeBackground = false
            ),
            "android.permission.REQUEST_INSTALL_PACKAGES" to PermissionRisk(
                permissionString = "android.permission.REQUEST_INSTALL_PACKAGES",
                displayName = "Install Packages",
                category = PermissionCategory.SYSTEM,
                weight = 20,
                dangerLevel = DangerLevel.DANGEROUS,
                explanation = "Allows the app to silently install other apps on your device.",
                realWorldExample = "Used by malware droppers to install additional malicious apps.",
                isRuntime = false,
                canBeBackground = true
            ),
            "android.permission.INTERNET" to PermissionRisk(
                permissionString = "android.permission.INTERNET",
                displayName = "Internet Access",
                category = PermissionCategory.NETWORK,
                weight = 10,
                dangerLevel = DangerLevel.MODERATE,
                explanation = "Allows the app to send and receive data over the internet.",
                realWorldExample = "Combined with other permissions, enables data exfiltration.",
                isRuntime = false,
                canBeBackground = true
            ),
            "android.permission.BLUETOOTH_SCAN" to PermissionRisk(
                permissionString = "android.permission.BLUETOOTH_SCAN",
                displayName = "Bluetooth Scan",
                category = PermissionCategory.NETWORK,
                weight = 12,
                dangerLevel = DangerLevel.MODERATE,
                explanation = "Scans for nearby Bluetooth devices, can infer your location.",
                realWorldExample = "Used to track movement through Bluetooth beacons in stores.",
                isRuntime = true,
                canBeBackground = true
            )
        )

        fun forPermission(permissionString: String): PermissionRisk? = ALL[permissionString]

        fun forPermissions(permissions: List<String>): List<PermissionRisk> =
            permissions.mapNotNull { ALL[it] }
                .sortedByDescending { it.weight }
    }
}

fun DangerLevel.toColor(): Int {
    return when (this) {
        DangerLevel.CRITICAL -> android.graphics.Color.parseColor("#CC0000")
        DangerLevel.DANGEROUS -> android.graphics.Color.parseColor("#FF4444")
        DangerLevel.MODERATE -> android.graphics.Color.parseColor("#FFA500")
        DangerLevel.NORMAL -> android.graphics.Color.parseColor("#44BB44")
    }
}