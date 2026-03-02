package com.sentinelx.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sentinelx.R
import com.sentinelx.shared.AlertConfig
import com.sentinelx.shared.Constants
import com.sentinelx.shared.MonitorEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PrivacyMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var alertConfig = AlertConfig.default()

    companion object {
        private val recentEvents = ArrayDeque<MonitorEvent>(50)

        fun getRecentEvents(limit: Int): List<MonitorEvent> =
            recentEvents.takeLast(limit).reversed()

        fun logEvent(event: MonitorEvent) {
            if (recentEvents.size >= 50) recentEvents.removeFirst()
            recentEvents.addLast(event)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(1, buildPersistentNotification())
        startPolling()
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                Constants.CHANNEL_ID_MONITOR,
                Constants.CHANNEL_NAME_MONITOR,
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Constants.CHANNEL_ID_ALERT,
                Constants.CHANNEL_NAME_ALERT,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts when camera or mic activates in background" }
        )
    }

    private fun buildPersistentNotification(): Notification =
        NotificationCompat.Builder(this, Constants.CHANNEL_ID_MONITOR)
            .setSmallIcon(R.drawable.logo_plain)
            .setContentTitle("SentinelX is watching")
            .setContentText("Monitoring camera, mic & location activity")
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun startPolling() {
        serviceScope.launch {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val pm = packageManager
            val packages = pm.getInstalledPackages(0)

            val opsToWatch = mapOf(
                AppOpsManager.OPSTR_CAMERA       to "android.permission.CAMERA",
                AppOpsManager.OPSTR_RECORD_AUDIO  to "android.permission.RECORD_AUDIO",
                AppOpsManager.OPSTR_FINE_LOCATION to "android.permission.ACCESS_FINE_LOCATION"
            )

            val lastSeenActive = mutableMapOf<String, Long>() // "pkg:perm" → last fire time

            while (isActive) {
                for (pkg in packages) {
                    val appInfo = pkg.applicationInfo ?: continue
                    val appName = appInfo.loadLabel(pm).toString()

                    for ((opStr, permString) in opsToWatch) {
                        try {
                            val uid = appInfo.uid
                            val mode = appOps.checkOpNoThrow(opStr, uid, pkg.packageName)
                            if (mode == AppOpsManager.MODE_ALLOWED) {
                                val key = "${pkg.packageName}:$permString"
                                val now = System.currentTimeMillis()
                                val lastFired = lastSeenActive[key] ?: 0L

                                // Only fire if we haven't alerted for this combo in last 10s
                                if (now - lastFired > 10_000L) {
                                    // Check if actually running using AppOps note time
                                    val isRecentlyUsed = isPermissionRecentlyUsed(
                                        appOps, opStr, uid, pkg.packageName
                                    )
                                    if (isRecentlyUsed) {
                                        lastSeenActive[key] = now
                                        val event = MonitorEvent.create(
                                            pkg.packageName, appName, permString,
                                            wasBackground = true
                                        )
                                        logEvent(event)

                                        if (alertConfig.shouldAlert(pkg.packageName, permString, true)) {
                                            fireAlert(appName, permString)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore per-app errors
                        }
                    }
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    private fun isPermissionRecentlyUsed(
        appOps: AppOpsManager, opStr: String, uid: Int, packageName: String
    ): Boolean {
        return try {
            // Use reflection to call the hidden queryOp / getOpsForPackage
            val method = appOps.javaClass.getMethod(
                "getOpsForPackage", Int::class.javaPrimitiveType, String::class.java, Array<String>::class.java
            )
            @Suppress("UNCHECKED_CAST")
            val pkgOpsList = method.invoke(appOps, uid, packageName, arrayOf(opStr)) as? List<*>
            if (pkgOpsList.isNullOrEmpty()) return false

            val pkgOps = pkgOpsList[0] ?: return false
            val getOps = pkgOps.javaClass.getMethod("getOps")
            val opEntries = getOps.invoke(pkgOps) as? List<*>
            if (opEntries.isNullOrEmpty()) return false

            val opEntry = opEntries[0] ?: return false

            val lastAccess = try {
                // API 30+ — getLastAccessTime(int flags)
                val m = opEntry.javaClass.getMethod("getLastAccessTime", Int::class.javaPrimitiveType)
                m.invoke(opEntry, 0x3) as? Long ?: 0L  // 0x3 = OP_FLAG_SELF | OP_FLAG_TRUSTED_PROXIED
            } catch (e: Exception) {
                try {
                    // Fallback — no-arg version
                    val m = opEntry.javaClass.getMethod("getLastAccessTime")
                    m.invoke(opEntry) as? Long ?: 0L
                } catch (e2: Exception) { 0L }
            }

            System.currentTimeMillis() - lastAccess < 5000L
        } catch (e: Exception) {
            false
        }
    }

    private fun fireAlert(appName: String, permissionString: String) {
        val emoji = when {
            permissionString.contains("CAMERA")       -> "📷"
            permissionString.contains("RECORD_AUDIO") -> "🎤"
            permissionString.contains("LOCATION")     -> "📍"
            else -> "⚠️"
        }
        val permLabel = permissionString.substringAfterLast(".")
            .replace("_", " ").lowercase()
            .replaceFirstChar { it.uppercase() }

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(this, Constants.CHANNEL_ID_ALERT)
                .setSmallIcon(R.drawable.logo_plain)
                .setContentTitle("$emoji $permLabel Activated")
                .setContentText("$appName is using $permLabel in background")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext[Job]?.cancel()
    }
}