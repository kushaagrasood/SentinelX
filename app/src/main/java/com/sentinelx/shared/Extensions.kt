package com.sentinelx.shared

import android.graphics.Color
import androidx.core.graphics.toColorInt

fun Int.toRiskColor(): Int {
    return when {
        this >= Constants.RISK_CRITICAL_THRESHOLD -> "#CC0000".toColorInt()
        this >= Constants.RISK_HIGH_THRESHOLD -> "#FF4444".toColorInt()
        this >= Constants.RISK_MEDIUM_THRESHOLD -> "#FFA500".toColorInt()
        else -> "#44BB44".toColorInt()
    }
}

fun Int.toRiskEmoji(): String {
    return when {
        this >= Constants.RISK_CRITICAL_THRESHOLD -> "🚨"
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

fun Long.toReadableTimestamp(): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(this))
}

fun Long.daysSince(): Long {
    return (System.currentTimeMillis() - this) / (1000 * 60 * 60 * 24)
}

fun List<AnomalyEvent>.hasType(type: AnomalyType): Boolean {
    return this.any { it.type == type }
}

fun AppInfo.isUnusedHighRisk(): Boolean {
    return riskLevel in listOf(Constants.RISK_HIGH, Constants.RISK_CRITICAL)
            && lastUsedTimestamp.daysSince() >= Constants.AUTO_REVOKE_UNUSED_DAYS
}

fun AppInfo.exportSummary(): String {
    return buildString {
        appendLine("=== SentinelX Privacy Report ===")
        appendLine("App: $appName ($packageName)")
        appendLine("Risk: ${riskScore.toRiskEmoji()} $riskLevel ($riskScore/100)")
        appendLine("Explanation: $riskExplanation")
        appendLine("Sensitive Permissions (${sensitivePermissions.size}):")
        sensitivePermissions.forEach { appendLine("  - $it") }
        if (anomalies.isNotEmpty()) {
            appendLine("Anomalies Detected (${anomalies.size}):")
            anomalies.forEach { appendLine("  ⚠️ ${it.type}: ${it.description}") }
        }
        if (autoRevokeSuggested) {
            appendLine("⚠️ Auto-revoke suggested: High risk, not used in ${lastUsedTimestamp.daysSince()} days")
        }
        appendLine("Battery usage today: ${"%.1f".format(batteryUsagePercent)}%")
        appendLine("Network usage today: ${networkUsageToday / 1024}KB")
        appendLine("Generated: ${System.currentTimeMillis().toReadableTimestamp()}")
    }
}