package com.sentinelx.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.sentinelx.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        loadSavedPrefs()
        setupTimePickers()
        setupSaveButton()
    }

    private fun loadSavedPrefs() {
        val prefs = getSharedPreferences("sentinelx_prefs", MODE_PRIVATE)
        findViewById<SwitchMaterial>(R.id.switchCamera).isChecked        = prefs.getBoolean("alert_camera", true)
        findViewById<SwitchMaterial>(R.id.switchMic).isChecked           = prefs.getBoolean("alert_mic", true)
        findViewById<SwitchMaterial>(R.id.switchLocation).isChecked      = prefs.getBoolean("alert_location", true)
        findViewById<SwitchMaterial>(R.id.switchBackgroundOnly).isChecked = prefs.getBoolean("background_only", true)
        findViewById<SwitchMaterial>(R.id.switchQuietHours).isChecked    = prefs.getBoolean("quiet_hours", false)
        findViewById<SwitchMaterial>(R.id.switchVibrate).isChecked       = prefs.getBoolean("vibrate", true)
        findViewById<SwitchMaterial>(R.id.switchSound).isChecked         = prefs.getBoolean("sound", true)
        findViewById<TextView>(R.id.tvQuietStart).text = prefs.getString("quiet_start", "23:00")
        findViewById<TextView>(R.id.tvQuietEnd).text   = prefs.getString("quiet_end", "07:00")
    }

    private fun setupTimePickers() {
        fun pickTime(tv: TextView, defaultHour: Int) {
            tv.setOnClickListener {
                val current = tv.text.toString().split(":").map { it.toIntOrNull() ?: 0 }
                TimePickerDialog(this, { _, h, m ->
                    tv.text = "%02d:%02d".format(h, m)
                }, current[0], current[1], true).show()
            }
        }
        pickTime(findViewById(R.id.tvQuietStart), 23)
        pickTime(findViewById(R.id.tvQuietEnd), 7)
    }

    private fun setupSaveButton() {
        findViewById<MaterialButton>(R.id.btnSaveSettings).setOnClickListener {
            val prefs = getSharedPreferences("sentinelx_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("alert_camera",    findViewById<SwitchMaterial>(R.id.switchCamera).isChecked)
                .putBoolean("alert_mic",       findViewById<SwitchMaterial>(R.id.switchMic).isChecked)
                .putBoolean("alert_location",  findViewById<SwitchMaterial>(R.id.switchLocation).isChecked)
                .putBoolean("background_only", findViewById<SwitchMaterial>(R.id.switchBackgroundOnly).isChecked)
                .putBoolean("quiet_hours",     findViewById<SwitchMaterial>(R.id.switchQuietHours).isChecked)
                .putString("quiet_start",      findViewById<TextView>(R.id.tvQuietStart).text.toString())
                .putString("quiet_end",        findViewById<TextView>(R.id.tvQuietEnd).text.toString())
                .putBoolean("vibrate",         findViewById<SwitchMaterial>(R.id.switchVibrate).isChecked)
                .putBoolean("sound",           findViewById<SwitchMaterial>(R.id.switchSound).isChecked)
                .apply()
            finish()
        }
    }
}