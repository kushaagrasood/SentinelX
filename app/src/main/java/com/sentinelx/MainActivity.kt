package com.sentinelx

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sentinelx.data.AppScanner
import com.sentinelx.data.UsageTracker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // THIS is the part that was missing! It tells the app what to do when it opens.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Check if Android allows us to see Usage Stats
        if (!hasUsageStatsPermission()) {
            Log.w("SentinelX_Test", "Usage Stats permission missing! Opening Settings...")
            // This opens the specific settings page where the user grants permission
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            // 2. Run your Data Layer test
            testDataLayer()
        }
    }

    private fun testDataLayer() {
        val appScanner = AppScanner(this)
        val usageTracker = UsageTracker(this)

        lifecycleScope.launch {
            Log.d("SentinelX_Test", "🚀 --- STARTING DATA LAYER TEST --- 🚀")

            try {
                // --- TEST 1: AppScanner ---
                Log.d("SentinelX_Test", "Scanning installed apps...")
                val apps = appScanner.getInstalledApps()
                Log.d("SentinelX_Test", "✅ Found ${apps.size} apps.")

                apps.take(5).forEach { app ->
                    Log.d("SentinelX_Test", "📱 App: ${app.appName} | Permissions Granted: ${app.grantedPermissions.size}")
                }

                // --- TEST 2: UsageTracker ---
                Log.d("SentinelX_Test", "Scanning sensor usage (Camera/Mic/Location)...")
                val usageStats = usageTracker.getUsageDurations()
                Log.d("SentinelX_Test", "✅ Found usage data for ${usageStats.size} apps.")

                usageStats.forEach { (packageName, durationMs) ->
                    val seconds = durationMs / 1000
                    Log.d("SentinelX_Test", "⏱️ Package: $packageName | Sensor active for: $seconds seconds")
                }

            } catch (e: Exception) {
                Log.e("SentinelX_Test", "❌ Error during test: ${e.message}", e)
            }

            Log.d("SentinelX_Test", "🏁 --- TEST COMPLETE --- 🏁")
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}