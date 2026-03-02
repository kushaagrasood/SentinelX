package com.sentinelx.shared

data class ScanSession(
    val sessionId: String,              // UUID
    val timestamp: Long,
    val apps: List<AppInfo>,
    val deviceRiskScore: Int,
    val deviceRiskLevel: String,
    val totalAnomalies: Int,
    val scanDurationMs: Long            // how long the scan took
) {
    companion object {
        fun create(
            apps: List<AppInfo>,
            scanDurationMs: Long
        ): ScanSession {
            val criticalAndHigh = apps.filter {
                it.riskLevel in listOf(Constants.RISK_CRITICAL, Constants.RISK_HIGH)
            }
            val deviceScore = if (apps.isEmpty()) 0
            else apps.sortedByDescending { it.riskScore }
                .take(10)
                .map { it.riskScore }
                .average()
                .toInt()
                .coerceAtMost(100)

            return ScanSession(
                sessionId = java.util.UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                apps = apps,
                deviceRiskScore = deviceScore,
                deviceRiskLevel = when {
                    deviceScore >= Constants.RISK_CRITICAL_THRESHOLD -> Constants.RISK_CRITICAL
                    deviceScore >= Constants.RISK_HIGH_THRESHOLD -> Constants.RISK_HIGH
                    deviceScore >= Constants.RISK_MEDIUM_THRESHOLD -> Constants.RISK_MEDIUM
                    else -> Constants.RISK_LOW
                },
                totalAnomalies = apps.sumOf { it.anomalies.size },
                scanDurationMs = scanDurationMs
            )
        }
    }
}

data class ScanDiff(
    val previousSession: ScanSession,
    val currentSession: ScanSession,
    val newHighRiskApps: List<AppInfo>,         // apps that became high/critical since last scan
    val resolvedApps: List<AppInfo>,            // apps that dropped in risk since last scan
    val newlyInstalledApps: List<AppInfo>,      // apps not in previous session
    val uninstalledApps: List<String>,          // packageNames no longer present
    val riskScoreDelta: Int,                    // positive = got worse, negative = improved
    val newAnomalies: List<AnomalyEvent>
) {
    companion object {
        fun compute(previous: ScanSession, current: ScanSession): ScanDiff {
            val prevPackages = previous.apps.map { it.packageName }.toSet()
            val currPackages = current.apps.map { it.packageName }.toSet()
            val prevRiskMap = previous.apps.associate { it.packageName to it.riskScore }

            val newlyInstalled = current.apps.filter { it.packageName !in prevPackages }
            val uninstalled = prevPackages - currPackages

            val newHighRisk = current.apps.filter { app ->
                val prevScore = prevRiskMap[app.packageName] ?: 0
                app.riskScore >= Constants.RISK_HIGH_THRESHOLD && prevScore < Constants.RISK_HIGH_THRESHOLD
            }

            val resolved = current.apps.filter { app ->
                val prevScore = prevRiskMap[app.packageName] ?: 0
                app.riskScore < Constants.RISK_HIGH_THRESHOLD && prevScore >= Constants.RISK_HIGH_THRESHOLD
            }

            val prevAnomalyIds = previous.apps
                .flatMap { it.anomalies }
                .map { it.timestamp }
                .toSet()

            val newAnomalies = current.apps
                .flatMap { it.anomalies }
                .filter { it.timestamp !in prevAnomalyIds }

            return ScanDiff(
                previousSession = previous,
                currentSession = current,
                newHighRiskApps = newHighRisk,
                resolvedApps = resolved,
                newlyInstalledApps = newlyInstalled,
                uninstalledApps = uninstalled.toList(),
                riskScoreDelta = current.deviceRiskScore - previous.deviceRiskScore,
                newAnomalies = newAnomalies
            )
        }
    }

    fun toSummaryText(): String {
        return buildString {
            appendLine("📊 Scan Comparison")
            appendLine("Previous: ${previousSession.timestamp.toReadableTimestamp()}")
            appendLine("Current:  ${currentSession.timestamp.toReadableTimestamp()}")
            appendLine()
            val arrow = if (riskScoreDelta > 0) "📈 Worse" else "📉 Improved"
            appendLine("Device Risk: $arrow by ${kotlin.math.abs(riskScoreDelta)} points")
            if (newlyInstalledApps.isNotEmpty())
                appendLine("🆕 New apps: ${newlyInstalledApps.joinToString { it.appName }}")
            if (uninstalledApps.isNotEmpty())
                appendLine("🗑️ Removed: ${uninstalledApps.joinToString()}")
            if (newHighRiskApps.isNotEmpty())
                appendLine("🔴 Newly high risk: ${newHighRiskApps.joinToString { it.appName }}")
            if (resolvedApps.isNotEmpty())
                appendLine("✅ Resolved: ${resolvedApps.joinToString { it.appName }}")
            if (newAnomalies.isNotEmpty())
                appendLine("⚠️ New anomalies: ${newAnomalies.size}")
        }
    }
}