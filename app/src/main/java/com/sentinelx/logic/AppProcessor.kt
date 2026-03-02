package com.sentinelx.logic

import com.sentinelx.shared.AppInfo
import com.sentinelx.shared.Constants
import com.sentinelx.shared.MonitorEvent
import com.sentinelx.shared.PrivacyReport
import com.sentinelx.shared.RawAppData

object AppProcessor {

    fun processApps(
        rawApps: List<RawAppData>
    ): Pair<List<AppInfo>, RiskSummary> {

        val processedApps = rawApps.map { raw ->

            val sensitivePermissions = raw.grantedPermissions.filter {
                Constants.SENSITIVE_PERMISSIONS.contains(it)
            }

            val score = RiskScoreEngine.calculateRiskScore(sensitivePermissions)
            val level = RiskScoreEngine.determineRiskLevel(score)

            AppInfo(
                packageName = raw.packageName,
                appName = raw.appName,
                icon = raw.icon,
                allPermissions = raw.permissions,
                grantedPermissions = raw.grantedPermissions,
                sensitivePermissions = sensitivePermissions,
                permissionUsageDurations = emptyMap(),
                riskScore = score,
                riskLevel = level,
                riskExplanation = RiskScoreEngine.generateRiskExplanation(
                    sensitivePermissions,
                    score
                )
            )
        }.sortedByDescending { it.riskScore }

        val summary = RiskSummary(
            totalApps = processedApps.size,
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

        val deviceRiskScore =
            if (topApps.isNotEmpty())
                topApps.map { it.riskScore }.average().toInt()
            else 0

        val criticalApps =
            apps.filter { it.riskLevel == Constants.RISK_HIGH }

        val highRiskApps =
            apps.filter { it.riskLevel == Constants.RISK_MEDIUM }

        val autoRevokeSuggestions =
            apps.filter { it.riskScore >= Constants.RISK_HIGH_THRESHOLD && it.permissionUsageDurations.isEmpty() }

        val topPermissionsByUsage =
            recentEvents.groupingBy { it.permission }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }
                .take(5)

        return PrivacyReport(
            deviceRiskScore = deviceRiskScore,
            criticalApps = criticalApps,
            highRiskApps = highRiskApps,
            autoRevokeSuggestions = autoRevokeSuggestions,
            topPermissionsByUsage = topPermissionsByUsage
        )
    }
}