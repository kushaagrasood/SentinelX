package com.sentinelx.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sentinelx.R
import com.sentinelx.logic.AppProcessor
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
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
        loadAppDetails(packageName)
    }

    private fun loadAppDetails(packageName: String) {
        val tvName        = findViewById<TextView>(R.id.tvDetailAppName)
        val tvPkg         = findViewById<TextView>(R.id.tvDetailPackage)
        val tvScore       = findViewById<TextView>(R.id.tvDetailRiskScore)
        val tvExplanation = findViewById<TextView>(R.id.tvDetailExplanation)
        val tvPerms       = findViewById<TextView>(R.id.tvDetailPermissions)
        val btnSettings   = findViewById<Button>(R.id.btnAppSettings)
        val btnShare      = findViewById<Button>(R.id.btnShare)

        lifecycleScope.launch {
            val app = withContext(Dispatchers.IO) {
                val (apps, _) = AppProcessor.processAppsWithContext(this@DetailActivity)
                apps.find { it.packageName == packageName }
            }

            app?.let {
                tvName.text = it.appName
                tvPkg.text  = it.packageName

                tvScore.text = "${it.riskScore.toRiskEmoji()} ${it.riskScore}/100  •  ${it.riskLevel}"
                tvScore.setTextColor(it.riskScore.toRiskColor())

                tvExplanation.text = it.riskExplanation

                if (it.sensitivePermissions.isNotEmpty()) {
                    tvPerms.text = it.sensitivePermissions.joinToString("\n\n") { perm ->
                        val risk = PermissionRisk.forPermission(perm)
                        val usage = it.permissionUsageDurations[perm]
                        val usageStr = if (usage != null && usage > 0)
                            "  ⏱ Used: ${usage.toReadableDuration()} today" else ""
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

                btnSettings.setOnClickListener {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${app.packageName}"))
                    )
                }

                btnShare.setOnClickListener {
                    startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, app.exportSummary())
                                .putExtra(Intent.EXTRA_SUBJECT, "SentinelX: ${app.appName}"),
                            "Share Report"
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}