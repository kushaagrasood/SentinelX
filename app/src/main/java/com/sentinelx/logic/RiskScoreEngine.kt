package com.sentinelx.logic

import com.sentinelx.shared.Constants
import kotlin.math.ln

object RiskScoreEngine {

    fun calculateRiskScore(grantedPermissions: List<String>): Int {
        if (grantedPermissions.isEmpty()) return 0

        // Step 1: Get raw weights for each granted sensitive permission
        val weights = grantedPermissions.mapNotNull { permission ->
            Constants.PERMISSION_WEIGHTS[permission]
        }.sortedDescending()

        if (weights.isEmpty()) return 0

        // Step 2: Diminishing returns — each additional permission adds less
        // First permission counts 100%, second 80%, third 60%, and so on down to 20%
        var score = 0.0
        val diminishingFactors = listOf(1.0, 0.8, 0.6, 0.45, 0.3, 0.2)

        weights.forEachIndexed { index, weight ->
            val factor = if (index < diminishingFactors.size)
                diminishingFactors[index]
            else
                0.15
            score += weight * factor
        }

        // Step 3: Dangerous combo bonuses — certain combinations are extra risky
        val hasCamera = grantedPermissions.any { it.contains("CAMERA") }
        val hasMic = grantedPermissions.any { it.contains("RECORD_AUDIO") }
        val hasLocation = grantedPermissions.any { it.contains("LOCATION") }
        val hasContacts = grantedPermissions.any { it.contains("CONTACTS") }
        val hasSms = grantedPermissions.any { it.contains("SMS") }
        val hasCallLog = grantedPermissions.any { it.contains("CALL_LOG") }
        val hasBackgroundLocation = grantedPermissions.any { it.contains("BACKGROUND_LOCATION") }
        val hasInternet = grantedPermissions.any { it.contains("INTERNET") }

        // Surveillance combo: camera + mic + internet = spyware profile
        if (hasCamera && hasMic && hasInternet) score += 8.0

        // Location tracking combo: background location + internet
        if (hasBackgroundLocation && hasInternet) score += 7.0

        // Data harvesting combo: contacts + sms + call log
        if (hasContacts && hasSms && hasCallLog) score += 6.0

        // Broad surveillance: camera/mic + location together
        if ((hasCamera || hasMic) && hasLocation) score += 5.0

        // Step 4: Apply logarithmic scaling to prevent runaway scores
        // This ensures even worst-case apps land around 85-90, not 100
        val scaled = 20.0 * ln(1.0 + score / 8.0)

        // Step 5: Clamp to 0-100
        return scaled.toInt().coerceIn(0, 100)
    }

    fun determineRiskLevel(score: Int): String {
        return when {
            score >= Constants.RISK_CRITICAL_THRESHOLD -> Constants.RISK_CRITICAL
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

        val level = determineRiskLevel(score)
        val count = sensitivePermissions.size
        val topPerms = sensitivePermissions.take(3).joinToString(", ") {
            it.substringAfterLast(".")
        }

        return when (level) {
            Constants.RISK_CRITICAL -> "⚠️ Critical risk: $count sensitive permissions including $topPerms. This app has a surveillance-level permission profile."
            Constants.RISK_HIGH -> "🔴 High risk: $count sensitive permissions including $topPerms."
            Constants.RISK_MEDIUM -> "🟠 Moderate risk: $count sensitive permissions including $topPerms."
            else -> "🟢 Low risk: $count sensitive permissions."
        }
    }
}