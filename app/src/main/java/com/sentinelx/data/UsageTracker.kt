package com.sentinelx.data

import android.app.AppOpsManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageTracker(private val context: Context) {

    private val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    suspend fun getUsageDurations(): Map<String, Long> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val usageMap = mutableMapOf<String, Long>()

        val opsToTrackStrings = arrayOf(
            AppOpsManager.OPSTR_CAMERA,
            AppOpsManager.OPSTR_RECORD_AUDIO,
            AppOpsManager.OPSTR_FINE_LOCATION
        )

        // Convert string ops to int codes using reflection as strOpToOp is a hidden API
        val opCodes = try {
            val strOpToOpMethod = AppOpsManager::class.java.getMethod("strOpToOp", String::class.java)
            opsToTrackStrings.mapNotNull { opStr ->
                try {
                    strOpToOpMethod.invoke(null, opStr) as? Int
                } catch (e: Exception) {
                    null
                }
            }.toIntArray()
        } catch (e: Exception) {
            intArrayOf()
        }

        if (opCodes.isEmpty()) return@withContext emptyMap()

        val now = System.currentTimeMillis()

        // Fetching the ops for all packages using reflection for the hidden getPackagesForOps API
        val allPkgOps = try {
            val method = appOps.javaClass.getMethod("getPackagesForOps", IntArray::class.java)
            @Suppress("UNCHECKED_CAST")
            method.invoke(appOps, opCodes) as? List<*>
        } catch (e: Exception) {
            null
        }

        for (pkg in packages) {
            var latestAccess = 0L

            try {
                // Find the ops for the current package in the pre-fetched list
                val currentPkgOps = allPkgOps?.find { pkgOps ->
                    try {
                        val getPackageName = pkgOps?.javaClass?.getMethod("getPackageName")
                        getPackageName?.invoke(pkgOps) == pkg.packageName
                    } catch (e: Exception) {
                        false
                    }
                }

                currentPkgOps?.let { pkgOps ->
                    val getOps = pkgOps.javaClass.getMethod("getOps")
                    val opsList = getOps.invoke(pkgOps) as? List<*>

                    opsList?.forEach { opEntry ->
                        if (opEntry != null) {
                            val time = try {
                                // Try with flags first (API 30+), 31 represents all flags (OP_FLAGS_ALL)
                                val getLastAccessTime = opEntry.javaClass.getMethod("getLastAccessTime", Int::class.javaPrimitiveType)
                                getLastAccessTime.invoke(opEntry, 31) as? Long ?: 0L
                            } catch (e: Exception) {
                                try {
                                    // Fallback to no-arg version for older SDKs
                                    val getLastAccessTime = opEntry.javaClass.getMethod("getLastAccessTime")
                                    getLastAccessTime.invoke(opEntry) as? Long ?: 0L
                                } catch (e2: Exception) {
                                    0L
                                }
                            }
                            if (time > latestAccess) {
                                latestAccess = time
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore errors for individual packages
            }

            if (latestAccess > 0) {
                // Calculate duration in ms since the last access
                usageMap[pkg.packageName] = now - latestAccess
            }
        }
        usageMap
    }
}
