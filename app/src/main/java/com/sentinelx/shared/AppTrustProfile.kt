package com.sentinelx.shared

enum class TrustLevel(val displayName: String, val emoji: String) {
    TRUSTED("Trusted", "✅"),
    UNKNOWN("Unknown Source", "⚠️"),
    SUSPICIOUS("Suspicious", "🚨"),
    SYSTEM("System App", "🔒")
}

enum class InstallSource(val displayName: String) {
    PLAY_STORE("Google Play Store"),
    AMAZON_STORE("Amazon Appstore"),
    SAMSUNG_STORE("Samsung Galaxy Store"),
    SIDELOADED("Sideloaded (APK)"),
    SYSTEM_PREINSTALLED("Pre-installed"),
    UNKNOWN("Unknown")
}

data class AppTrustProfile(
    val packageName: String,
    val installSource: InstallSource,
    val trustLevel: TrustLevel,
    val isSystemApp: Boolean,
    val isDebuggable: Boolean,          // true = dev/test build, suspicious in prod
    val requestsRootAccess: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val trustReasons: List<String>,     // human readable reasons for trust level
    val suspicionReasons: List<String>  // human readable reasons for suspicion
) {
    companion object {
        val TRUSTED_INSTALLERS = setOf(
            "com.android.vending",          // Google Play Store
            "com.amazon.venezia",           // Amazon Appstore
            "com.sec.android.app.samsungapps" // Samsung Galaxy Store
        )

        fun computeTrustLevel(
            installSource: InstallSource,
            isSystemApp: Boolean,
            isDebuggable: Boolean,
            requestsRootAccess: Boolean
        ): TrustLevel {
            if (isSystemApp) return TrustLevel.SYSTEM
            if (requestsRootAccess || isDebuggable) return TrustLevel.SUSPICIOUS
            if (installSource == InstallSource.SIDELOADED) return TrustLevel.UNKNOWN
            if (installSource in listOf(
                    InstallSource.PLAY_STORE,
                    InstallSource.AMAZON_STORE,
                    InstallSource.SAMSUNG_STORE
                )
            ) return TrustLevel.TRUSTED
            return TrustLevel.UNKNOWN
        }

        fun resolveInstallSource(installerPackageName: String?): InstallSource {
            return when (installerPackageName) {
                "com.android.vending" -> InstallSource.PLAY_STORE
                "com.amazon.venezia" -> InstallSource.AMAZON_STORE
                "com.sec.android.app.samsungapps" -> InstallSource.SAMSUNG_STORE
                null -> InstallSource.SIDELOADED
                else -> InstallSource.UNKNOWN
            }
        }
    }
}

// Extension to get trust color
fun TrustLevel.toColor(): Int {
    return when (this) {
        TrustLevel.TRUSTED -> android.graphics.Color.parseColor("#44BB44")
        TrustLevel.UNKNOWN -> android.graphics.Color.parseColor("#FFA500")
        TrustLevel.SUSPICIOUS -> android.graphics.Color.parseColor("#FF4444")
        TrustLevel.SYSTEM -> android.graphics.Color.parseColor("#4488FF")
    }
}