package com.sentinelx.shared

data class RiskSnapshot(
    val timestamp: Long,       // when this score was recorded
    val riskScore: Int,        // score at that time
    val riskLevel: String      // "LOW", "MEDIUM", "HIGH", "CRITICAL"
)