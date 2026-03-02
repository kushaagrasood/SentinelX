package com.sentinelx.data

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkTrafficTracker(private val context: Context) {

    suspend fun getGhostDataUsage(): Map<String, Double> = withContext(Dispatchers.IO) {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)

        // Map of PackageName to Megabytes (MB) used
        val usageMap = mutableMapOf<String, Double>()

        // Look at traffic over the last 24 hours
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24 * 60 * 60 * 1000)

        for (pkg in packages) {
            val uid = pkg.applicationInfo?.uid ?: continue

            try {
                // Querying WiFi Data usage (Can also do TRANSPORT_CELLULAR)
                val networkStats = networkStatsManager.queryDetailsForUid(
                    NetworkCapabilities.TRANSPORT_WIFI,
                    "",
                    startTime,
                    endTime,
                    uid
                )

                var totalBytes = 0L
                val bucket = android.app.usage.NetworkStats.Bucket()

                while (networkStats.hasNextBucket()) {
                    networkStats.getNextBucket(bucket)
                    totalBytes += bucket.rxBytes + bucket.txBytes // Receive + Transmit
                }
                networkStats.close()

                // Convert Bytes to Megabytes (MB)
                val totalMegabytes = totalBytes / (1024.0 * 1024.0)

                // Only flag it if it's secretly transferring more than 5 MB of data
                if (totalMegabytes > 5.0) {
                    usageMap[pkg.packageName] = totalMegabytes
                }
            } catch (e: Exception) {
                continue
            }
        }
        usageMap
    }
}