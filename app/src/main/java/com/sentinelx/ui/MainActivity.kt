package com.sentinelx.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sentinelx.R
import com.sentinelx.data.PrivacyMonitorService
import com.sentinelx.logic.AppProcessor
import com.sentinelx.shared.MonitorEvent
import com.sentinelx.shared.ScanDiff
import com.sentinelx.shared.ScanSession
import com.sentinelx.shared.toReadableTimestamp
import com.sentinelx.shared.toRiskColor
import com.sentinelx.shared.toRiskEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()
        checkUsageAccessAndProceed()
        setupQuickControls()

        // Start the monitor service
        val serviceIntent = Intent(this, PrivacyMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setupToolbarButtons()
    }

    // ── Permission checks ──

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }
    }

    private fun checkUsageAccessAndProceed() {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            showUsageAccessDialog()
        } else {
            loadApps()
        }
    }

    private fun showUsageAccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("SentinelX needs Usage Access permission to track how long apps use sensitive permissions.\n\nGrant it in the next screen under your app name.")
            .setPositiveButton("Grant Access") { _, _ ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .setNegativeButton("Skip") { _, _ ->
                loadApps() // still loads, just without usage durations
            }
            .show()
    }

    // ── Load apps ──

    private fun loadApps() {
        val recycler  = findViewById<RecyclerView>(R.id.recyclerView)
        val tvLoading = findViewById<TextView>(R.id.tvLoading)
        val tvHigh    = findViewById<TextView>(R.id.tvHighCount)
        val tvMed     = findViewById<TextView>(R.id.tvMedCount)
        val tvLow     = findViewById<TextView>(R.id.tvLowCount)

        recycler.layoutManager = LinearLayoutManager(this)
        tvLoading.text = "🔍 Scanning..."

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            val (apps, summary) = withContext(Dispatchers.IO) {
                AppProcessor.processAppsWithContext(this@MainActivity)
            }

            val scanDuration = System.currentTimeMillis() - startTime

            tvLoading.text = ""
            // HIGH card shows CRITICAL + HIGH combined
            tvHigh.text = "${summary.criticalCount + summary.highCount}"
            tvMed.text  = "${summary.mediumCount}"
            tvLow.text  = "${summary.lowCount}"

            recycler.adapter = AppListAdapter(apps) { app ->
                startActivity(
                    Intent(this@MainActivity, DetailActivity::class.java)
                        .putExtra(DetailActivity.EXTRA_PACKAGE_NAME, app.packageName)
                )
            }

            val currentSession = ScanSession.create(apps, scanDuration)
            updateLastScanCard(currentSession)

            val previousSession = loadPreviousSession()
            if (previousSession != null) {
                showScanDiffBanner(ScanDiff.compute(previousSession, currentSession))
            }
            saveScanSession(currentSession)

            showRecentActivity(PrivacyMonitorService.getRecentEvents(10))
        }
    }

    private fun loadPreviousSession(): ScanSession? = null  // TODO: persist with Room/SharedPrefs
    private fun saveScanSession(session: ScanSession) {}     // TODO: persist with Room/SharedPrefs

    // ── UI updates ──

    private fun updateLastScanCard(session: ScanSession) {
        val card = findViewById<View>(R.id.cardLastScan) ?: return
        card.visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvScanDeviceScore)?.apply {
            text = "${session.deviceRiskScore}/100"
            setTextColor(session.deviceRiskScore.toRiskColor())
        }
        findViewById<TextView>(R.id.tvScanRiskLevel)?.text =
            "${session.deviceRiskScore.toRiskEmoji()} ${session.deviceRiskLevel}"
        findViewById<TextView>(R.id.tvScanDuration)?.text =
            "Scan took ${session.scanDurationMs}ms • ${session.apps.size} apps"
    }

    private fun showScanDiffBanner(diff: ScanDiff) {
        val banner = findViewById<androidx.cardview.widget.CardView>(R.id.cardScanDiff) ?: return
        banner.visibility = View.VISIBLE
        findViewById<TextView>(R.id.tvScanDiffText)?.text = diff.toSummaryText()
        findViewById<TextView>(R.id.btnDismissDiff)?.setOnClickListener {
            banner.visibility = View.GONE
        }
    }

    private fun showRecentActivity(events: List<MonitorEvent>) {
        val container = findViewById<LinearLayout>(R.id.recentActivityContainer) ?: return
        val section   = findViewById<View>(R.id.sectionRecentActivity) ?: return
        if (events.isEmpty()) return

        section.visibility = View.VISIBLE
        container.removeAllViews()

        events.take(10).forEach { event ->
            val row = layoutInflater.inflate(R.layout.item_monitor_event, container, false)
            row.findViewById<TextView>(R.id.tvEventAppName).text = event.appName
            row.findViewById<TextView>(R.id.tvEventCategory).text = event.category.emoji
            row.findViewById<TextView>(R.id.tvEventPermission).text =
                event.permissionTriggered.substringAfterLast(".")
            row.findViewById<TextView>(R.id.tvEventTimestamp).text =
                event.timestamp.toReadableTimestamp()
            val bgBadge = row.findViewById<TextView>(R.id.tvEventBgBadge)
            bgBadge.visibility = if (event.wasBackground) View.VISIBLE else View.GONE
            container.addView(row)
        }
    }

    private fun exportFullReport() {
        val shareIntent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "SentinelX Privacy Report\n\nRun a full scan first.")
            .putExtra(Intent.EXTRA_SUBJECT, "SentinelX Full Privacy Report")
        startActivity(Intent.createChooser(shareIntent, "Export Report"))
    }

    // ── Setup ──

    private fun setupToolbarButtons() {
        findViewById<TextView>(R.id.btnExportReport)?.setOnClickListener { exportFullReport() }
        findViewById<TextView>(R.id.btnOpenSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupQuickControls() {
        findViewById<Button>(R.id.btnToggleCamera)?.setOnClickListener {
            try { startActivity(Intent("android.settings.CAMERA_SETTINGS")) }
            catch (e: Exception) { startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS)) }
        }
        findViewById<Button>(R.id.btnToggleMic)?.setOnClickListener {
            try { startActivity(Intent("android.settings.MICROPHONE_SETTINGS")) }
            catch (e: Exception) { startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS)) }
        }
        findViewById<Button>(R.id.btnToggleLocation)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload after returning from Usage Access settings
    }
}