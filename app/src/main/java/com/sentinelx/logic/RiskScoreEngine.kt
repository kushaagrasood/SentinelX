package com.sentinelx.logic

import com.sentinelx.shared.Constants

object RiskScoreEngine {

    fun calculateRiskScore(grantedPermissions: List<String>): Int {
        var score = 0
        grantedPermissions.forEach { permission ->
            score += Constants.PERMISSION_WEIGHTS[permission] ?: 0
        }
        return score.coerceAtMost(100)
    }

    fun determineRiskLevel(score: Int): String {
        return when {
            score >= Constants.RISK_CRITICAL_THRESHOLD -> Constants.RISK_CRITICAL  // ← ADD THIS
            score >= Constants.RISK_HIGH_THRESHOLD -> Constants.RISK_HIGH
            score >= Constants.RISK_MEDIUM_THRESHOLD -> Constants.RISK_MEDIUM
            else -> Constants.RISK_LOW
        }
    }

    fun generateRiskExplanation(sensitivePermissions: List<String>, score: Int): String {
        if (score == 0) return "No sensitive permissions detected."
        val topPerms = sensitivePermissions
            .sortedByDescending { Constants.PERMISSION_WEIGHTS[it] ?: 0 }
            .take(3)
            .joinToString(", ") { it.substringAfterLast(".") }
        return "Risk driven by: $topPerms (${sensitivePermissions.size} sensitive permissions total)"
    }
}