package com.sentinelx.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sentinelx.R
import com.sentinelx.data.AppScanner
import com.sentinelx.logic.AppProcessor
import com.sentinelx.shared.PermissionRisk
import com.sentinelx.shared.exportSummary
import com.sentinelx.shared.toRiskColor
import com.sentinelx.shared.toRiskEmoji
import com.sentinelx.shared.toReadableDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
        loadAppDetails(packageName)
    }

    private fun loadAppDetails(packageName: String) {
        val tvName  = findViewById<TextView>(R.id.tvDetailAppName)
        val tvPkg   = findViewById<TextView>(R.id.tvDetailPackage)
        val tvScore = findViewById<TextView>(R.id.tvDetailRiskScore)
        val tvPerms = findViewById<TextView>(R.id.tvDetailPermissions)
        val tvExplanation = findViewById<TextView>(R.id.tvDetailExplanation)
        val btnAppSettings = findViewById<Button>(R.id.btnAppSettings)
        val btnShare = findViewById<Button>(R.id.btnShare)

        lifecycleScope.launch {
            val app = withContext(Dispatchers.IO) {
                val rawApps = AppScanner(this@DetailActivity).getInstalledApps()
                val (apps, _) = AppProcessor.processApps(rawApps)
                apps.find { it.packageName == packageName }
            }

            app?.let {
                tvName.text = it.appName
                tvPkg.text = it.packageName

                tvScore.text = "${it.riskScore.toRiskEmoji()} ${it.riskScore}/100  ${it.riskLevel}"
                tvScore.setTextColor(it.riskScore.toRiskColor())

                tvExplanation.text = it.riskExplanation

                // Show rich permission info if available, otherwise plain list
                if (it.sensitivePermissions.isNotEmpty()) {
                    val permText = it.sensitivePermissions.joinToString("\n\n") { perm ->
                        val risk = PermissionRisk.forPermission(perm)
                        if (risk != null) {
                            "• ${risk.displayName} ${risk.dangerLevel.name}\n  ${risk.explanation}"
                        } else {
                            "• ${perm.substringAfterLast(".")}"
                        }
                    }
                    tvPerms.text = permText
                } else {
                    tvPerms.text = "No sensitive permissions detected."
                }

                btnAppSettings.setOnClickListener {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${app.packageName}"))
                    )
                }

                btnShare.setOnClickListener {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, app.exportSummary())
                        .putExtra(Intent.EXTRA_SUBJECT, "SentinelX Report: ${app.appName}")
                    startActivity(Intent.createChooser(shareIntent, "Share Report"))
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}