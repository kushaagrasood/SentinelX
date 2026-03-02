package com.sentinelx.logic

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.sentinelx.data.AppScanner
import com.sentinelx.data.UsageTracker
import com.sentinelx.shared.AppInfo
import com.sentinelx.shared.Constants
import com.sentinelx.shared.MonitorEvent
import com.sentinelx.shared.PrivacyReport
import com.sentinelx.shared.RawAppData

object AppProcessor {

    suspend fun processAppsWithContext(context: Context): Pair<List<AppInfo>, RiskSummary> {
        val rawApps = AppScanner(context).getInstalledApps()
        val usageDurations = try { UsageTracker(context).getUsageDurations() } catch (e: Exception) { emptyMap() }

        // Collect installer info for reputation scoring
        val pm = context.packageManager
        val installerMap = rawApps.associate { raw ->
            raw.packageName to try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    pm.getInstallSourceInfo(raw.packageName).installingPackageName
                else
                    @Suppress("DEPRECATION") pm.getInstallerPackageName(raw.packageName)
            } catch (e: Exception) { null }
        }

        val systemFlags = rawApps.associate { raw ->
            raw.packageName to try {
                val ai = pm.getApplicationInfo(raw.packageName, 0)
                (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (e: Exception) { false }
        }

        return processApps(rawApps, usageDurations, installerMap, systemFlags)
    }

    fun processApps(
        rawApps: List<RawAppData>,
        usageDurations: Map<String, Long> = emptyMap(),
        installerMap: Map<String, String?> = emptyMap(),
        systemFlagMap: Map<String, Boolean> = emptyMap()
    ): Pair<List<AppInfo>, RiskSummary> {

        val processedApps = rawApps.map { raw ->

            val sensitivePermissions = raw.grantedPermissions.filter {
                Constants.SENSITIVE_PERMISSIONS.contains(it)
            }

            val isSystem = systemFlagMap[raw.packageName] ?: false
            val installer = installerMap[raw.packageName]

            // Use the new multi-factor RiskEngine
            val riskResult = RiskEngine.calculateFinalRisk(
                packageName              = raw.packageName,
                grantedPermissions       = raw.grantedPermissions,
                sensitivePermissions     = sensitivePermissions,
                permissionUsageDurations = emptyMap(), // filled below
                isSystemApp              = isSystem,
                installerPackageName     = installer
            )

            val permUsage = Constants.APPOPS_MAP.keys.associate { perm ->
                perm to (usageDurations[raw.packageName] ?: 0L)
            }.filterValues { it > 0L }

            AppInfo(
                packageName              = raw.packageName,
                appName                  = raw.appName,
                icon                     = raw.icon,
                allPermissions           = raw.permissions,
                grantedPermissions       = raw.grantedPermissions,
                sensitivePermissions     = sensitivePermissions,
                permissionUsageDurations = permUsage,
                riskScore                = riskResult.score,
                riskLevel                = riskResult.level,
                riskExplanation          = riskResult.explanation,
                riskHistory              = emptyList(),
                recentlyGrantedPermissions = emptyList(),
                recentlyRevokedPermissions = emptyList(),
                usesInternet             = raw.grantedPermissions.contains("android.permission.INTERNET"),
                networkUsageToday        = 0L,
                batteryUsagePercent      = 0.0,
                anomalies                = emptyList(),
                autoRevokeSuggested      = riskResult.score >= 61,
                lastUsedTimestamp        = raw.lastUpdateTime
            )
        }.sortedByDescending { it.riskScore }

        val summary = RiskSummary(
            totalApps    = processedApps.size,
            criticalCount = 0, // new engine uses HIGH/MEDIUM/LOW only per spec
            highCount    = processedApps.count { it.riskLevel == "HIGH" },
            mediumCount  = processedApps.count { it.riskLevel == "MEDIUM" },
            lowCount     = processedApps.count { it.riskLevel == "LOW" }
        )

        return Pair(processedApps, summary)
    }

    fun generateReport(apps: List<AppInfo>, recentEvents: List<MonitorEvent>): PrivacyReport {
        val topApps = apps.sortedByDescending { it.riskScore }.take(10)
        val deviceScore = if (topApps.isNotEmpty()) topApps.map { it.riskScore }.average().toInt() else 0
        return PrivacyReport(
            generatedAt          = System.currentTimeMillis(),
            totalAppsScanned     = apps.size,
            criticalApps         = apps.filter { it.riskLevel == "HIGH" && it.riskScore >= 80 },
            highRiskApps         = apps.filter { it.riskLevel == "HIGH" },
            totalAnomalies       = apps.sumOf { it.anomalies.size },
            recentMonitorEvents  = recentEvents.takeLast(50),
            autoRevokeSuggestions = apps.filter { it.autoRevokeSuggested },
            topPermissionsByUsage = recentEvents.groupingBy { it.permissionTriggered }
                .eachCount().mapValues { it.value.toLong() },
            deviceRiskScore      = deviceScore,
            deviceRiskLevel      = RiskEngine.determineLevel(deviceScore)
        )
    }
}