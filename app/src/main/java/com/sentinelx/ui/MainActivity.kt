package com.sentinelx.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sentinelx.R
import com.sentinelx.data.PrivacyMonitorService
import com.sentinelx.logic.AppProcessor
import com.sentinelx.shared.MonitorEvent
import com.sentinelx.shared.ScanSession
import com.sentinelx.shared.toReadableTimestamp
import com.sentinelx.shared.toRiskColor
import com.sentinelx.shared.toRiskEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var currentSession: ScanSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()
        startMonitorService()
        setupToolbarButtons()
        setupQuickControls()
        checkUsageAccessAndLoad()
    }

    // ── Permission + Service bootstrap ───────────────────────────────────────

    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
            )
        }
    }

    private fun startMonitorService() {
        val intent = Intent(this, PrivacyMonitorService::class.java)
        startForegroundService(intent)
    }

    private fun checkUsageAccessAndLoad() {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            AlertDialog.Builder(this)
                .setTitle("Usage Access Required")
                .setMessage(
                    "SentinelX needs Usage Access to track how long apps use your camera, " +
                            "mic and location.\n\nOn the next screen, find SentinelX and enable it."
                )
                .setPositiveButton("Grant Access") { _, _ ->
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                .setNegativeButton("Skip") { _, _ -> loadApps() }
                .show()
        } else {
            loadApps()
        }
    }

    // ── Main scan ────────────────────────────────────────────────────────────

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

            tvHigh.text = summary.highCount.toString()
            tvMed.text  = summary.mediumCount.toString()
            tvLow.text  = summary.lowCount.toString()

            recycler.adapter = AppListAdapter(apps) { app ->
                startActivity(
                    Intent(this@MainActivity, DetailActivity::class.java)
                        .putExtra(DetailActivity.EXTRA_PACKAGE_NAME, app.packageName)
                )
            }

            currentSession = ScanSession.create(apps, scanDuration)
            currentSession?.let { session ->
                AppProcessor.loadLastSessionMeta(this@MainActivity)?.let { prevMeta ->
                    val delta = session.deviceRiskScore - prevMeta.first
                    if (delta != 0) {
                        showSimpleDiff(delta)
                    }
                }
                
                updateLastScanCard(session)
                AppProcessor.saveSession(this@MainActivity, session)
            }

            showRecentActivity(PrivacyMonitorService.getRecentEvents(10))
        }
    }

    private fun showSimpleDiff(delta: Int) {
        val card = findViewById<View>(R.id.cardScanDiff) ?: return
        val tvDiff = findViewById<TextView>(R.id.tvScanDiffText) ?: return
        val btnDismiss = findViewById<TextView>(R.id.btnDismissDiff) ?: return

        val text = if (delta > 0) 
            "⚠️ Device risk increased by $delta points!" 
            else "✅ Device risk improved by ${-delta} points!"
        
        tvDiff.text = text
        card.visibility = View.VISIBLE
        btnDismiss.setOnClickListener { card.visibility = View.GONE }
    }

    // ── UI updaters ──────────────────────────────────────────────────────────

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
            "Scanned ${session.apps.size} apps in ${session.scanDurationMs}ms"
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
            row.findViewById<TextView>(R.id.tvEventBgBadge).visibility =
                if (event.wasBackground) View.VISIBLE else View.GONE
            container.addView(row)
        }
    }

    // ── Quick Controls popup ─────────────────────────────────────────────────

    private fun setupQuickControls() {
        findViewById<Button>(R.id.btnToggleCamera)?.setOnClickListener {
            showPrivacyControlPopup(
                emoji = "📷",
                title = "Camera Access",
                threat = "Apps with camera access can take photos or record video silently, even in the background.",
                tips = listOf(
                    "Only grant camera to apps that genuinely need it",
                    "Background camera use is a stalkerware red flag",
                    "Prefer 'While using the app' over 'Always allow'"
                ),
                settingLabel = "Manage Camera Permissions"
            ) {
                try { startActivity(Intent("android.settings.CAMERA_SETTINGS")) }
                catch (e: Exception) { startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS)) }
            }
        }

        findViewById<Button>(R.id.btnToggleMic)?.setOnClickListener {
            showPrivacyControlPopup(
                emoji = "🎤",
                title = "Microphone Access",
                threat = "Apps with microphone access can record audio at any time, including conversations in the background.",
                tips = listOf(
                    "Messaging apps legitimately need mic access",
                    "Games, utilities and keyboards rarely need it",
                    "SentinelX alerts you when mic activates in background"
                ),
                settingLabel = "Manage Microphone Permissions"
            ) {
                try { startActivity(Intent("android.settings.MICROPHONE_SETTINGS")) }
                catch (e: Exception) { startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS)) }
            }
        }

        findViewById<Button>(R.id.btnToggleLocation)?.setOnClickListener {
            showPrivacyControlPopup(
                emoji = "📍",
                title = "Location Access",
                threat = "Background location lets apps track your movements continuously without you opening them.",
                tips = listOf(
                    "Deny 'Allow all the time' unless truly necessary",
                    "Most apps only need location 'While using the app'",
                    "Weather and maps rarely need background location"
                ),
                settingLabel = "Manage Location Settings"
            ) {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
    }

    private fun showPrivacyControlPopup(
        emoji: String,
        title: String,
        threat: String,
        tips: List<String>,
        settingLabel: String,
        onProceed: () -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_privacy_control, null)

        dialogView.findViewById<TextView>(R.id.tvPopupEmoji).text = emoji
        dialogView.findViewById<TextView>(R.id.tvPopupTitle).text = title
        dialogView.findViewById<TextView>(R.id.tvPopupThreat).text = threat

        val tipsContainer = dialogView.findViewById<LinearLayout>(R.id.llPopupTips)
        tips.forEach { tip ->
            val tv = TextView(this).apply {
                text = "•  $tip"
                textSize = 13f
                setTextColor("#AAFFFFFF".toColorInt())
                setPadding(0, 8, 0, 0)
                setLineSpacing(0f, 1.3f)
            }
            tipsContainer.addView(tv)
        }

        val proceedBtn = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPopupProceed)
        proceedBtn.text = settingLabel

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        proceedBtn.setOnClickListener { dialog.dismiss(); onProceed() }
        dialogView.findViewById<TextView>(R.id.btnPopupDismiss)
            .setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // ── Report export ────────────────────────────────────────────────────────

    private fun exportFullReport() {
        val session = currentSession
        val reportText = if (session != null) {
            AppProcessor.generateReport(session.apps, PrivacyMonitorService.getRecentEvents(50))
                .toShareableText()
        } else {
            "SentinelX Privacy Report\n\nNo scan data available. Run a scan first."
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, reportText)
                    .putExtra(Intent.EXTRA_SUBJECT, "SentinelX Privacy Report"),
                "Export Report"
            )
        )
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

    private fun setupToolbarButtons() {
        findViewById<TextView>(R.id.btnExportReport)?.setOnClickListener { exportFullReport() }
        findViewById<TextView>(R.id.btnOpenSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        showRecentActivity(PrivacyMonitorService.getRecentEvents(10))
    }
}