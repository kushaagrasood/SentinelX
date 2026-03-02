package com.sentinelx.shared

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,

    // Permissions
    val allPermissions: List<String>,
    val grantedPermissions: List<String>,
    val sensitivePermissions: List<String>,
    val permissionUsageDurations: Map<String, Long>,   // permission → ms used today

    // Risk
    val riskScore: Int,                                // 0–100
    val riskLevel: String,                             // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val riskExplanation: String,
    val riskHistory: List<RiskSnapshot>,               // score over time

    // Permission change history
    val recentlyGrantedPermissions: List<String>,      // granted in last 7 days
    val recentlyRevokedPermissions: List<String>,      // revoked in last 7 days

    // Network
    val usesInternet: Boolean,                         // declared INTERNET permission
    val networkUsageToday: Long,                       // bytes transmitted today (if available)

    // Battery
    val batteryUsagePercent: Double,                   // % battery used today

    // Anomaly
    val anomalies: List<AnomalyEvent>,                 // detected suspicious behavior

    // Suggestions
    val autoRevokeSuggested: Boolean,                  // true if high risk + rarely used
    val lastUsedTimestamp: Long                        // last time app was opened
)