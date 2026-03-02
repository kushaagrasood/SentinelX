package com.sentinelx.ui

import com.sentinelx.R
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupQuickControls()
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

        // TODO: uncomment this block after Member 2 finishes AppProcessor
        /*
        lifecycleScope.launch {
            val processor = com.sentinelx.logic.AppProcessor(this@MainActivity)
            val apps      = withContext(Dispatchers.IO) { processor.getProcessedApps() }
            val summary   = processor.getSummary(apps)

            tvLoading.text = ""
            tvHigh.text = "🔴 ${summary.highCount}\nHIGH"
            tvMed.text  = "🟠 ${summary.medCount}\nMEDIUM"
            tvLow.text  = "🟢 ${summary.lowCount}\nLOW"

            recycler.adapter = AppListAdapter(apps) { app ->
                val intent = Intent(this@MainActivity, DetailActivity::class.java)
                intent.putExtra("extra_package_name", app.packageName)
                startActivity(intent)
            }
        }
        */

        // TEMPORARY placeholder so app doesn't crash while others are coding
        tvLoading.text = "⏳ Waiting for other modules..."
        tvHigh.text = "🔴 0\nHIGH"
        tvMed.text  = "🟠 0\nMEDIUM"
        tvLow.text  = "🟢 0\nLOW"
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