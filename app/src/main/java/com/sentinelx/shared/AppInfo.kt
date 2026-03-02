package com.sentinelx.shared

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val allPermissions: List<String>,
    val grantedPermissions: List<String>,
    val sensitivePermissions: List<String>,
    val permissionUsageDurations: Map<String, Long>,
    val riskScore: Int,
    val riskLevel: String,
    val riskExplanation: String
)