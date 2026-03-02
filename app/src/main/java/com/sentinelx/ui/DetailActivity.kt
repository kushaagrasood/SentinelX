package com.sentinelx.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sentinelx.R
import com.sentinelx.data.AppScanner
import com.sentinelx.logic.AppProcessor
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
        val tvName = findViewById<TextView>(R.id.tvDetailAppName)
        val tvPkg = findViewById<TextView>(R.id.tvDetailPackage)
        val tvScore = findViewById<TextView>(R.id.tvDetailRiskScore)
        val tvPerms = findViewById<TextView>(R.id.tvDetailPermissions)

        lifecycleScope.launch {
            val rawApps = withContext(Dispatchers.IO) {
                AppScanner(this@DetailActivity).getInstalledApps()
            }
            val (apps, _) = AppProcessor.processApps(rawApps)
            val app = apps.find { it.packageName == packageName }

            app?.let {
                tvName.text = it.appName
                tvPkg.text = it.packageName
                tvScore.text = "${it.riskScore}/100"
                tvPerms.text = if (it.sensitivePermissions.isNotEmpty()) {
                    it.sensitivePermissions.joinToString("\n")
                } else {
                    "No sensitive permissions detected."
                }
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}