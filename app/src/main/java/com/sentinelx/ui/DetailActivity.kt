package com.sentinelx.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.sentinelx.R
import com.sentinelx.logic.AppProcessor
import com.sentinelx.shared.AppTrustProfile
import com.sentinelx.shared.PermissionRisk
import com.sentinelx.shared.exportSummary
import com.sentinelx.shared.toReadableDuration
import com.sentinelx.shared.toRiskColor
import com.sentinelx.shared.toRiskEmoji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        loadAppDetails(intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return)
    }

    private fun loadAppDetails(packageName: String) {
        val tvName        = findViewById<TextView>(R.id.tvDetailAppName)
        val tvPkg         = findViewById<TextView>(R.id.tvDetailPackage)
        val tvScore       = findViewById<TextView>(R.id.tvDetailRiskScore)
        val tvExplanation = findViewById<TextView>(R.id.tvDetailExplanation)
        val tvPerms       = findViewById<TextView>(R.id.tvDetailPermissions)
        val tvTrustBadge  = findViewById<TextView>(R.id.tvTrustBadge)
        val tvNetwork     = findViewById<TextView>(R.id.tvNetworkUsage)
        val btnSettings   = findViewById<MaterialButton>(R.id.btnAppSettings)
        val btnShare      = findViewById<MaterialButton>(R.id.btnShare)

        tvName.text = "Loading…"

        lifecycleScope.launch {
            val pm = packageManager

            val (apps, _) = withContext(Dispatchers.IO) {
                AppProcessor.processAppsWithContext(this@DetailActivity)
            }
            val app = apps.find { it.packageName == packageName } ?: return@launch

            // ── Header ──
            tvName.text = app.appName
            tvPkg.text  = app.packageName
            tvScore.text = "${app.riskScore.toRiskEmoji()} ${app.riskScore}/100  •  ${app.riskLevel}"
            tvScore.setTextColor(app.riskScore.toRiskColor())
            tvExplanation.text = app.riskExplanation

            // ── Trust Profile badge ──
            val installerPkg = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    pm.getInstallSourceInfo(app.packageName).installingPackageName
                else @Suppress("DEPRECATION") pm.getInstallerPackageName(app.packageName)
            } catch (e: Exception) { null }

            val source = AppTrustProfile.resolveInstallSource(installerPkg)
            tvTrustBadge.text = "${source.displayName}"
            tvTrustBadge.setTextColor(
                when (source) {
                    com.sentinelx.shared.InstallSource.PLAY_STORE,
                    com.sentinelx.shared.InstallSource.AMAZON_STORE,
                    com.sentinelx.shared.InstallSource.SAMSUNG_STORE -> Color.parseColor("#44BB44")
                    com.sentinelx.shared.InstallSource.SIDELOADED    -> Color.parseColor("#FF4444")
                    else                                              -> Color.parseColor("#FFA500")
                }
            )

            // ── Network usage ──
            if (app.networkUsageToday > 0L) {
                tvNetwork.text = "📶 Network today: ${app.networkUsageToday / 1024}KB"
                tvNetwork.visibility = android.view.View.VISIBLE
            } else {
                tvNetwork.visibility = android.view.View.GONE
            }

            // ── Permissions list ──
            if (app.sensitivePermissions.isNotEmpty()) {
                tvPerms.text = app.sensitivePermissions.joinToString("\n\n") { perm ->
                    val risk = PermissionRisk.forPermission(perm)
                    val usageMs = app.permissionUsageDurations[perm] ?: 0L
                    val usageStr = if (usageMs > 0) "\n  ⏱ Used: ${usageMs.toReadableDuration()} today" else ""
                    if (risk != null) {
                        "• ${risk.displayName}  [${risk.dangerLevel.name}]\n" +
                                "  ${risk.explanation}\n" +
                                "  ⚠ ${risk.realWorldExample}$usageStr"
                    } else {
                        "• ${perm.substringAfterLast(".")}$usageStr"
                    }
                }
            } else {
                tvPerms.text = "✅ No sensitive permissions detected."
            }

            // ── Buttons ──
            btnSettings.setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:${app.packageName}"))
                )
            }
            btnShare.setOnClickListener {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, app.exportSummary())
                            .putExtra(Intent.EXTRA_SUBJECT, "SentinelX: ${app.appName}"),
                        "Share Report"
                    )
                )
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}