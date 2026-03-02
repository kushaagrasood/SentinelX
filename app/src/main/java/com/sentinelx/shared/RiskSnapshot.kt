package com.sentinelx.shared

data class RiskSnapshot(
    val timestamp: Long,
    val riskScore: Int,
    val riskLevel: String
)