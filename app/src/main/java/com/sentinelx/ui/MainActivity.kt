package com.sentinelx.ui

import com.sentinelx.R
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sentinelx.shared.*
import com.sentinelx.data.PrivacyMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupQuickControls()
        setupToolbarButtons()
        loadApps()
    }

    private fun loadApps() {
        val recycler  = findViewById<RecyclerView>(R.id.recyclerView)
        val tvLoading = findViewById<TextView>(R.id.tvLoading)
        val tvHigh    = findViewById<TextView>(R.id.tvHighCount)
        val tvMed     = findViewById<TextView>(R.id.tvMedCount)
        val tvLow     = findViewById<TextView>(R.id.tvLowCount)

        recycler.layoutManager = LinearLayoutManager(this)
        tvLoading.text = "🔍 Scanning apps..."

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()

            val rawApps = withContext(Dispatchers.IO) {
                com.sentinelx.data.AppScanner(this@MainActivity).getInstalledApps()
            }
            val (apps, summary) = com.sentinelx.logic.AppProcessor.processApps(rawApps)
            val scanDuration = System.currentTimeMillis() - startTime

            tvLoading.text = ""
            tvHigh.text = "${summary.highCount}"
            tvMed.text  = "${summary.mediumCount}"
            tvLow.text  = "${summary.lowCount}"

            recycler.adapter = AppListAdapter(apps) { app ->
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                intent.putExtra(DetailActivity.EXTRA_PACKAGE_NAME, app.packageName)
                startActivity(intent)
            }

            val currentSession = ScanSession.create(apps, scanDuration)
            updateLastScanCard(currentSession)

            val previousSession = loadPreviousSession()
            if (previousSession != null) {
                val diff = ScanDiff.compute(previousSession, currentSession)
                showScanDiffBanner(diff)
            }
            saveScanSession(currentSession)

            val recentEvents = PrivacyMonitorService.getRecentEvents(10)
            showRecentActivity(recentEvents)
        }
    }

    private fun loadPreviousSession(): ScanSession? = null

    private fun saveScanSession(session: ScanSession) { }

    fun updateLastScanCard(session: ScanSession) {
        val card = findViewById<View>(R.id.cardLastScan) ?: return
        card.visibility = View.VISIBLE

        val tvScanScore    = findViewById<TextView>(R.id.tvScanDeviceScore) ?: return
        val tvScanDuration = findViewById<TextView>(R.id.tvScanDuration) ?: return
        val tvScanLevel    = findViewById<TextView>(R.id.tvScanRiskLevel) ?: return

        tvScanScore.text = "${session.deviceRiskScore}/100"
        tvScanScore.setTextColor(session.deviceRiskScore.toRiskColor())
        tvScanLevel.text = "${session.deviceRiskScore.toRiskEmoji()} ${session.deviceRiskLevel}"
        tvScanDuration.text = "Scan took ${session.scanDurationMs}ms • ${session.apps.size} apps"
    }

    fun showScanDiffBanner(diff: ScanDiff) {
        val banner     = findViewById<androidx.cardview.widget.CardView>(R.id.cardScanDiff) ?: return
        val tvDiff     = findViewById<TextView>(R.id.tvScanDiffText) ?: return
        val btnDismiss = findViewById<TextView>(R.id.btnDismissDiff) ?: return

        banner.visibility = View.VISIBLE
        tvDiff.text = diff.toSummaryText()
        btnDismiss.setOnClickListener { banner.visibility = View.GONE }
    }

    fun showRecentActivity(events: List<MonitorEvent>) {
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
            if (event.wasBackground) {
                bgBadge.visibility = View.VISIBLE
                bgBadge.text = "BG"
            } else {
                bgBadge.visibility = View.GONE
            }

            container.addView(row)
        }
    }

    private fun exportFullReport() {
        val placeholderText = "SentinelX Privacy Report\n\nRun a full scan first to generate report."
        val shareIntent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, placeholderText)
            .putExtra(Intent.EXTRA_SUBJECT, "SentinelX Full Privacy Report")
        startActivity(Intent.createChooser(shareIntent, "Export Report"))
    }

    private fun setupToolbarButtons() {
        findViewById<TextView>(R.id.btnExportReport)?.setOnClickListener {
            exportFullReport()
        }
        findViewById<TextView>(R.id.btnOpenSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupQuickControls() {
        findViewById<Button>(R.id.btnToggleCamera).setOnClickListener {
            try {
                startActivity(Intent("android.settings.CAMERA_SETTINGS"))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))
            }
        }

        findViewById<Button>(R.id.btnToggleMic).setOnClickListener {
            try {
                startActivity(Intent("android.settings.MICROPHONE_SETTINGS"))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))
            }
        }

        findViewById<Button>(R.id.btnToggleLocation).setOnClickListener {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }
}