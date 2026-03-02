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
            score >= Constants.RISK_HIGH_THRESHOLD -> Constants.RISK_HIGH
            score >= Constants.RISK_MEDIUM_THRESHOLD -> Constants.RISK_MEDIUM
            else -> Constants.RISK_LOW
        }
    }

    fun generateRiskExplanation(
        sensitivePermissions: List<String>,
        score: Int
    ): String {
        if (score == 0) return "No sensitive permissions detected."

        return "App uses ${sensitivePermissions.size} sensitive permissions: ${
            sensitivePermissions.joinToString(", ")
        }"
    }
}