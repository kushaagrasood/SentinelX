package com.sentinelx.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// A new data class just for your advanced threats
data class ThreatIntel(
    val packageName: String,
    val appName: String,
    val isSideloaded: Boolean,
    val hasDeviceAdmin: Boolean,
    val installerSource: String?
)

class AdvancedThreatScanner(private val context: Context) {

    suspend fun scanDeepThreats(): List<ThreatIntel> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val threatList = mutableListOf<ThreatIntel>()

        for (pkg in packages) {
            val appInfo = pkg.applicationInfo ?: continue

            // Ignore official System Apps
            if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) continue

            val appName = appInfo.loadLabel(pm).toString()

            // 1. Detect Sideloading (Where did this app come from?)
            val installer = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(pkg.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(pkg.packageName)
                }
            } catch (e: Exception) { null }

            // If it didn't come from the Play Store ("com.android.vending"), it is HIGH RISK
            val isSideloaded = installer != "com.android.vending" && installer != null

            // 2. Detect "Undeletable" Device Admin privileges
            val permissions = pkg.requestedPermissions?.toList() ?: emptyList()
            val hasAdmin = permissions.contains("android.permission.BIND_DEVICE_ADMIN")

            // If we find either threat, log it!
            if (isSideloaded || hasAdmin) {
                threatList.add(
                    ThreatIntel(pkg.packageName, appName, isSideloaded, hasAdmin, installer)
                )
            }
        }
        threatList
    }
}