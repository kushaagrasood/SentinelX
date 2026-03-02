package com.sentinelx.logic

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.sentinelx.data.AppScanner
import com.sentinelx.data.NetworkTrafficTracker
import com.sentinelx.data.UsageTracker
import com.sentinelx.shared.AppInfo
import com.sentinelx.shared.Constants
import com.sentinelx.shared.MonitorEvent
import com.sentinelx.shared.PrivacyReport
import com.sentinelx.shared.RawAppData
import com.sentinelx.shared.ScanSession
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object AppProcessor {

    // ── Full scan with all data sources ──────────────────────────────────────
    suspend fun processAppsWithContext(context: Context): Pair<List<AppInfo>, RiskSummary> {
        val pm = context.packageManager

        return coroutineScope {
            val rawAppsDeferred     = async { AppScanner(context).getInstalledApps() }
            val usageDeferred       = async {
                try { UsageTracker(context).getUsageDurations() } catch (e: Exception) { emptyMap() }
            }
            val networkDeferred     = async {
                try { NetworkTrafficTracker(context).getGhostDataUsage() } catch (e: Exception) { emptyMap() }
            }

            val rawApps      = rawAppsDeferred.await()
            val usageDurations  = usageDeferred.await()
            val networkUsage    = networkDeferred.await()

            // Collect installer + system flag for every package
            val installerMap = rawApps.associate { raw ->
                raw.packageName to try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        pm.getInstallSourceInfo(raw.packageName).installingPackageName
                    else
                        @Suppress("DEPRECATION") pm.getInstallerPackageName(raw.packageName)
                } catch (e: Exception) { null }
            }
            val systemFlagMap = rawApps.associate { raw ->
                raw.packageName to try {
                    (pm.getApplicationInfo(raw.packageName, 0).flags and ApplicationInfo.FLAG_SYSTEM) != 0
                } catch (e: Exception) { false }
            }

            processApps(rawApps, usageDurations, networkUsage, installerMap, systemFlagMap)
        }
    }

    // ── Core processing — also callable from tests / without Context ─────────
    fun processApps(
        rawApps: List<RawAppData>,
        usageDurations: Map<String, Long>      = emptyMap(),
        networkUsage: Map<String, Double>       = emptyMap(),
        installerMap: Map<String, String?>      = emptyMap(),
        systemFlagMap: Map<String, Boolean>     = emptyMap()
    ): Pair<List<AppInfo>, RiskSummary> {

        val processedApps = rawApps.map { raw ->
            val sensitivePermissions = raw.grantedPermissions.filter {
                Constants.SENSITIVE_PERMISSIONS.contains(it)
            }
            val permUsage = Constants.APPOPS_MAP.keys
                .associateWith { usageDurations[raw.packageName] ?: 0L }
                .filterValues { it > 0L }

            val isSystem  = systemFlagMap[raw.packageName] ?: false
            val installer = installerMap[raw.packageName]

            val risk = RiskEngine.calculateFinalRisk(
                packageName              = raw.packageName,
                grantedPermissions       = raw.grantedPermissions,
                sensitivePermissions     = sensitivePermissions,
                permissionUsageDurations = permUsage,
                isSystemApp              = isSystem,
                installerPackageName     = installer
            )

            val networkBytes = ((networkUsage[raw.packageName] ?: 0.0) * 1024 * 1024).toLong()

            AppInfo(
                packageName              = raw.packageName,
                appName                  = raw.appName,
                icon                     = raw.icon,
                allPermissions           = raw.permissions,
                grantedPermissions       = raw.grantedPermissions,
                sensitivePermissions     = sensitivePermissions,
                permissionUsageDurations = permUsage,
                riskScore                = risk.score,
                riskLevel                = risk.level,
                riskExplanation          = risk.explanation,
                riskHistory              = emptyList(),
                recentlyGrantedPermissions = emptyList(),
                recentlyRevokedPermissions = emptyList(),
                usesInternet             = raw.grantedPermissions.contains("android.permission.INTERNET"),
                networkUsageToday        = networkBytes,
                batteryUsagePercent      = 0.0,
                anomalies                = emptyList(),
                autoRevokeSuggested      = risk.score >= 61,
                lastUsedTimestamp        = raw.lastUpdateTime
            )
        }.sortedByDescending { it.riskScore }

        val summary = RiskSummary(
            totalApps    = processedApps.size,
            criticalCount = processedApps.count { it.riskScore >= 80 },
            highCount    = processedApps.count { it.riskLevel == "HIGH" },
            mediumCount  = processedApps.count { it.riskLevel == "MEDIUM" },
            lowCount     = processedApps.count { it.riskLevel == "LOW" }
        )

        return Pair(processedApps, summary)
    }

    fun generateReport(apps: List<AppInfo>, recentEvents: List<MonitorEvent>): PrivacyReport {
        val topScore = if (apps.isNotEmpty())
            apps.sortedByDescending { it.riskScore }.take(10).map { it.riskScore }.average().toInt()
        else 0
        return PrivacyReport(
            generatedAt          = System.currentTimeMillis(),
            totalAppsScanned     = apps.size,
            criticalApps         = apps.filter { it.riskScore >= 80 },
            highRiskApps         = apps.filter { it.riskLevel == "HIGH" },
            totalAnomalies       = apps.sumOf { it.anomalies.size },
            recentMonitorEvents  = recentEvents.takeLast(50),
            autoRevokeSuggestions = apps.filter { it.autoRevokeSuggested },
            topPermissionsByUsage = recentEvents.groupingBy { it.permissionTriggered }
                .eachCount().mapValues { it.value.toLong() },
            deviceRiskScore      = topScore,
            deviceRiskLevel      = RiskEngine.determineLevel(topScore)
        )
    }

    // ── ScanSession persistence via SharedPreferences (JSON-lite) ────────────
    fun saveSession(context: Context, session: ScanSession) {
        val prefs = context.getSharedPreferences("sentinelx_scan", Context.MODE_PRIVATE)
        // Store minimal summary only (Drawables can't be serialized)
        val data = buildString {
            append("${session.sessionId}|${session.timestamp}|${session.deviceRiskScore}|")
            append("${session.deviceRiskLevel}|${session.totalAnomalies}|${session.scanDurationMs}|")
            append(session.apps.size)
        }
        prefs.edit().putString("last_session_meta", data).apply()
    }

    fun loadLastSessionMeta(context: Context): Triple<Int, String, Long>? {
        // Returns (deviceRiskScore, deviceRiskLevel, timestamp) — enough for diff banner
        val prefs = context.getSharedPreferences("sentinelx_scan", Context.MODE_PRIVATE)
        val raw = prefs.getString("last_session_meta", null) ?: return null
        return try {
            val parts = raw.split("|")
            Triple(parts[2].toInt(), parts[3], parts[1].toLong())
        } catch (e: Exception) { null }
    }
}