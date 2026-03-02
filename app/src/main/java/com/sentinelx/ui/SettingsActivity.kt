package com.sentinelx.ui

import com.sentinelx.R
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupToolbar()
        setupSaveButton()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSaveButton() {
        val switchCamera          = findViewById<SwitchMaterial>(R.id.switchCamera)
        val switchMic             = findViewById<SwitchMaterial>(R.id.switchMic)
        val switchLocation        = findViewById<SwitchMaterial>(R.id.switchLocation)
        val switchBackgroundOnly  = findViewById<SwitchMaterial>(R.id.switchBackgroundOnly)
        val switchQuietHours      = findViewById<SwitchMaterial>(R.id.switchQuietHours)
        val switchVibrate         = findViewById<SwitchMaterial>(R.id.switchVibrate)
        val switchSound           = findViewById<SwitchMaterial>(R.id.switchSound)
        val tvQuietStart          = findViewById<TextView>(R.id.tvQuietStart)
        val tvQuietEnd            = findViewById<TextView>(R.id.tvQuietEnd)

        findViewById<MaterialButton>(R.id.btnSaveSettings).setOnClickListener {
            // Save settings to SharedPreferences
            val prefs = getSharedPreferences("sentinelx_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("alert_camera", switchCamera.isChecked)
                .putBoolean("alert_mic", switchMic.isChecked)
                .putBoolean("alert_location", switchLocation.isChecked)
                .putBoolean("background_only", switchBackgroundOnly.isChecked)
                .putBoolean("quiet_hours", switchQuietHours.isChecked)
                .putString("quiet_start", tvQuietStart.text.toString())
                .putString("quiet_end", tvQuietEnd.text.toString())
                .putBoolean("vibrate", switchVibrate.isChecked)
                .putBoolean("sound", switchSound.isChecked)
                .apply()

            finish()
        }
    }
}