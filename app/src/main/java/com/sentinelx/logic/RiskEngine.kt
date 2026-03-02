package com.sentinelx.logic

import android.content.pm.ApplicationInfo
import com.sentinelx.shared.AppInfo
import com.sentinelx.shared.RawAppData
import kotlin.math.abs
import kotlin.math.roundToInt

data class RiskResult(
    val score: Int,
    val level: String,
    val permissionScore: Int,
    val behaviorScore: Int,
    val reputationScore: Int,
    val contextScore: Int,
    val explanation: String
)

object RiskEngine {

    // ── Weights ──────────────────────────────────────────────
    private const val W_PERMISSION  = 0.35
    private const val W_BEHAVIOR    = 0.30
    private const val W_REPUTATION  = 0.20
    private const val W_CONTEXT     = 0.15

    // ── Thresholds ───────────────────────────────────────────
    private const val THRESHOLD_HIGH   = 61
    private const val THRESHOLD_MEDIUM = 31

    // ── Permission weights ───────────────────────────────────
    private val PERMISSION_WEIGHTS = mapOf(
        "android.permission.RECORD_AUDIO"            to 25,
        "android.permission.CAMERA"                  to 25,
        "android.permission.ACCESS_FINE_LOCATION"    to 20,
        "android.permission.READ_SMS"                to 20,
        "android.permission.SEND_SMS"                to 20,
        "android.permission.ACCESS_COARSE_LOCATION"  to 15,
        "android.permission.READ_CONTACTS"           to 15,
        "android.permission.READ_CALL_LOG"           to 15,
        "android.permission.WRITE_EXTERNAL_STORAGE"  to  5,
        "android.permission.READ_EXTERNAL_STORAGE"   to  5
    )

    // ── Behavior rule scores ─────────────────────────────────
    private const val B_BACKGROUND_MIC       = 30
    private const val B_BACKGROUND_CAMERA    = 30
    private const val B_FREQUENT_LOCATION    = 20
    private const val B_MULTI_SENSITIVE      = 20
    private const val B_IMMEDIATE_USE        = 15

    // ── Context rule scores ──────────────────────────────────
    private const val C_SCREEN_OFF           = 30
    private const val C_NIGHT_USE            = 20
    private const val C_DEVICE_IDLE          = 20

    // ─────────────────────────────────────────────────────────
    // 1. PERMISSION SCORE
    // ─────────────────────────────────────────────────────────
    fun calculatePermissionScore(grantedPermissions: List<String>): Int {
        val total = grantedPermissions.sumOf { PERMISSION_WEIGHTS[it] ?: 0 }
        return total.coerceAtMost(100)
    }

    // ─────────────────────────────────────────────────────────
    // 2. BEHAVIOR SCORE
    // Real data: use permissionUsageDurations + sensitivePermissions
    // Fallback: deterministic random seeded by packageName
    // ─────────────────────────────────────────────────────────
    fun calculateBehaviorScore(
        packageName: String,
        sensitivePermissions: List<String>,
        permissionUsageDurations: Map<String, Long>
    ): Int {
        val hasRealData = permissionUsageDurations.isNotEmpty()

        if (hasRealData) {
            var score = 0
            val hasCam  = permissionUsageDurations.containsKey("android.permission.CAMERA")
            val hasMic  = permissionUsageDurations.containsKey("android.permission.RECORD_AUDIO")
            val hasLoc  = permissionUsageDurations.containsKey("android.permission.ACCESS_FINE_LOCATION")

            if (hasMic)  score += B_BACKGROUND_MIC
            if (hasCam)  score += B_BACKGROUND_CAMERA
            if (hasLoc)  score += B_FREQUENT_LOCATION

            // Multiple sensitive permissions = escalation signal
            if (sensitivePermissions.size >= 5) score += B_MULTI_SENSITIVE

            // Heavy user = immediate use pattern heuristic
            val totalUsageMs = permissionUsageDurations.values.sum()
            if (totalUsageMs > 0) score += B_IMMEDIATE_USE

            return score.coerceAtMost(100)
        }

        // Deterministic fallback — seeded by package name so same app always gets same score
        return deterministicRandom(packageName, seed = 7, min = 10, max = 50)
    }

    // ─────────────────────────────────────────────────────────
    // 3. REPUTATION SCORE
    // ─────────────────────────────────────────────────────────
    fun calculateReputationScore(
        packageName: String,
        isSystemApp: Boolean,
        installerPackageName: String?
    ): Int {
        if (isSystemApp) return 5

        return when (installerPackageName) {
            "com.android.vending"                -> deterministicRandom(packageName, seed = 3, min = 10, max = 20)
            "com.amazon.venezia",
            "com.sec.android.app.samsungapps"   -> deterministicRandom(packageName, seed = 3, min = 10, max = 20)
            null                                -> 40   // sideloaded — no installer record
            else                                -> 30   // unknown installer
        }
    }

    // ─────────────────────────────────────────────────────────
    // 4. CONTEXT SCORE
    // Real detection requires system APIs not available without root.
    // Using deterministic simulation seeded by packageName.
    // ─────────────────────────────────────────────────────────
    fun calculateContextScore(packageName: String): Int {
        // Simulated — deterministic per package
        return deterministicRandom(packageName, seed = 13, min = 5, max = 25)
    }

    // ─────────────────────────────────────────────────────────
    // 5. FINAL SCORE
    // ─────────────────────────────────────────────────────────
    fun calculateFinalRisk(
        packageName: String,
        grantedPermissions: List<String>,
        sensitivePermissions: List<String>,
        permissionUsageDurations: Map<String, Long>,
        isSystemApp: Boolean,
        installerPackageName: String?
    ): RiskResult {
        val p = calculatePermissionScore(grantedPermissions)
        val b = calculateBehaviorScore(packageName, sensitivePermissions, permissionUsageDurations)
        val r = calculateReputationScore(packageName, isSystemApp, installerPackageName)
        val c = calculateContextScore(packageName)

        val raw = W_PERMISSION * p + W_BEHAVIOR * b + W_REPUTATION * r + W_CONTEXT * c
        val score = raw.roundToInt().coerceIn(0, 100)
        val level = determineLevel(score)

        return RiskResult(
            score = score,
            level = level,
            permissionScore = p,
            behaviorScore = b,
            reputationScore = r,
            contextScore = c,
            explanation = buildExplanation(level, p, b, r, c, sensitivePermissions, installerPackageName)
        )
    }

    // ─────────────────────────────────────────────────────────
    // Convenience overload — accepts AppInfo directly
    // ─────────────────────────────────────────────────────────
    fun calculateFinalRisk(
        app: AppInfo,
        isSystemApp: Boolean = false,
        installerPackageName: String? = null
    ): RiskResult = calculateFinalRisk(
        packageName              = app.packageName,
        grantedPermissions       = app.grantedPermissions,
        sensitivePermissions     = app.sensitivePermissions,
        permissionUsageDurations = app.permissionUsageDurations,
        isSystemApp              = isSystemApp,
        installerPackageName     = installerPackageName
    )

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────
    fun determineLevel(score: Int): String = when {
        score >= THRESHOLD_HIGH   -> "HIGH"
        score >= THRESHOLD_MEDIUM -> "MEDIUM"
        else                      -> "LOW"
    }

    private fun buildExplanation(
        level: String,
        p: Int, b: Int, r: Int, c: Int,
        sensitivePerms: List<String>,
        installerPackageName: String?
    ): String {
        val dominant = listOf(
            "Permissions" to (p * W_PERMISSION),
            "Behavior"    to (b * W_BEHAVIOR),
            "Reputation"  to (r * W_REPUTATION),
            "Context"     to (c * W_CONTEXT)
        ).maxByOrNull { it.second }?.first ?: "Permissions"

        val sourceNote = when (installerPackageName) {
            null -> " App was sideloaded (no Play Store record)."
            "com.android.vending" -> ""
            else -> " Installed from unknown source."
        }

        val permNote = if (sensitivePerms.isNotEmpty())
            " Top risk: ${sensitivePerms.take(2).joinToString { it.substringAfterLast(".") }}."
        else ""

        return when (level) {
            "HIGH"   -> "🔴 HIGH: $dominant is the primary risk factor.$permNote$sourceNote (P:$p B:$b R:$r C:$c)"
            "MEDIUM" -> "🟠 MEDIUM: Moderate risk driven by $dominant.$permNote$sourceNote (P:$p B:$b R:$r C:$c)"
            else     -> "🟢 LOW: No major risk signals detected.$permNote (P:$p B:$b R:$r C:$c)"
        }
    }

    // Deterministic pseudo-random: same package always returns same value in [min, max]
    private fun deterministicRandom(input: String, seed: Int, min: Int, max: Int): Int {
        val hash = abs(input.hashCode() xor (seed * 2654435761.toInt()))
        return min + (hash % (max - min + 1))
    }
}