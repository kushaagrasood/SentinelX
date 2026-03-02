package com.sentinelx.shared

fun Int.toRiskColor(): Int {
    return when {
        this >= Constants.RISK_HIGH_THRESHOLD -> android.graphics.Color.parseColor("#FF4444")
        this >= Constants.RISK_MEDIUM_THRESHOLD -> android.graphics.Color.parseColor("#FFA500")
        else -> android.graphics.Color.parseColor("#44BB44")
    }
}

fun Int.toRiskEmoji(): String {
    return when {
        this >= Constants.RISK_HIGH_THRESHOLD -> "🔴"
        this >= Constants.RISK_MEDIUM_THRESHOLD -> "🟠"
        else -> "🟢"
    }
}

fun Long.toReadableDuration(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}