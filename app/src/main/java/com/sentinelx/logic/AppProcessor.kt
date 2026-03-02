package com.sentinelx.logic

import android.content.Context
import com.sentinelx.data.AppScanner
import com.sentinelx.data.UsageTracker
import com.sentinelx.shared.AppInfo
import com.sentinelx.shared.Constants
import com.sentinelx.shared.MonitorEvent
import com.sentinelx.shared.PrivacyReport
import com.sentinelx.shared.RawAppData

object AppProcessor {

    // Main entry point — call this from UI with context
    suspend fun processAppsWithContext(context: Context): Pair<List<AppInfo>, RiskSummary> {
        val rawApps = AppScanner(context).getInstalledApps()
        val usageDurations = try {
            UsageTracker(context).getUsageDurations()
        } catch (e: Exception) {
            emptyMap()
        }
        return processApps(rawApps, usageDurations)
    }

    fun processApps(
        rawApps: List<RawAppData>,
        usageDurations: Map<String, Long> = emptyMap()
    ): Pair<List<AppInfo>, RiskSummary> {

        val processedApps = rawApps.map { raw ->

            val sensitivePermissions = raw.grantedPermissions.filter {
                Constants.SENSITIVE_PERMISSIONS.contains(it)
            }

            val score = RiskScoreEngine.calculateRiskScore(sensitivePermissions)
            val level = RiskScoreEngine.determineRiskLevel(score)

            // Map per-permission usage durations for this app
            val permUsage = Constants.APPOPS_MAP.entries
                .associate { (perm, _) ->
                    perm to (usageDurations[raw.packageName] ?: 0L)
                }
                .filterValues { it > 0L }

            AppInfo(
                packageName = raw.packageName,
                appName = raw.appName,
                icon = raw.icon,
                allPermissions = raw.permissions,
                grantedPermissions = raw.grantedPermissions,
                sensitivePermissions = sensitivePermissions,
                permissionUsageDurations = permUsage,
                riskScore = score,
                riskLevel = level,
                riskExplanation = RiskScoreEngine.generateRiskExplanation(sensitivePermissions, score),
                riskHistory = emptyList(),
                recentlyGrantedPermissions = emptyList(),
                recentlyRevokedPermissions = emptyList(),
                usesInternet = raw.grantedPermissions.contains("android.permission.INTERNET"),
                networkUsageToday = 0L,
                batteryUsagePercent = 0.0,
                anomalies = emptyList(),
                autoRevokeSuggested = score >= Constants.RISK_HIGH_THRESHOLD,
                lastUsedTimestamp = raw.lastUpdateTime
            )
        }.sortedByDescending { it.riskScore }

        val summary = RiskSummary(
            totalApps = processedApps.size,
            criticalCount = processedApps.count { it.riskLevel == Constants.RISK_CRITICAL },
            highCount = processedApps.count { it.riskLevel == Constants.RISK_HIGH },
            mediumCount = processedApps.count { it.riskLevel == Constants.RISK_MEDIUM },
            lowCount = processedApps.count { it.riskLevel == Constants.RISK_LOW }
        )

        return Pair(processedApps, summary)
    }

    fun generateReport(
        apps: List<AppInfo>,
        recentEvents: List<MonitorEvent>
    ): PrivacyReport {
        val topApps = apps.sortedByDescending { it.riskScore }.take(10)
        val deviceRiskScore = if (topApps.isNotEmpty())
            topApps.map { it.riskScore }.average().toInt() else 0

        return PrivacyReport(
            generatedAt = System.currentTimeMillis(),
            totalAppsScanned = apps.size,
            criticalApps = apps.filter { it.riskLevel == Constants.RISK_CRITICAL },
            highRiskApps = apps.filter { it.riskLevel == Constants.RISK_HIGH },
            totalAnomalies = apps.sumOf { it.anomalies.size },
            recentMonitorEvents = recentEvents.takeLast(50),
            autoRevokeSuggestions = apps.filter { it.autoRevokeSuggested },
            topPermissionsByUsage = recentEvents
                .groupingBy { it.permissionTriggered }
                .eachCount()
                .mapValues { it.value.toLong() },
            deviceRiskScore = deviceRiskScore,
            deviceRiskLevel = RiskScoreEngine.determineRiskLevel(deviceRiskScore)
        )
    }
}