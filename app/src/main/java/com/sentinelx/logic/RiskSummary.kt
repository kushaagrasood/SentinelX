package com.sentinelx.logic

data class RiskSummary(
    val totalApps: Int,
    val criticalCount: Int,   // ← ADD
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int
)