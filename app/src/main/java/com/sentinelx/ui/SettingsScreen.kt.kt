package com.sentinelx.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinelx.ui.theme.*

@Composable
fun SettingsScreen(viewModel: ScanViewModel) {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("sentinelx_prefs", android.content.Context.MODE_PRIVATE)

    var alertCamera    by remember { mutableStateOf(prefs.getBoolean("alert_camera", true)) }
    var alertMic       by remember { mutableStateOf(prefs.getBoolean("alert_mic", true)) }
    var alertLocation  by remember { mutableStateOf(prefs.getBoolean("alert_location", true)) }
    var bgOnly         by remember { mutableStateOf(prefs.getBoolean("background_only", true)) }
    var quietHours     by remember { mutableStateOf(prefs.getBoolean("quiet_hours", false)) }
    var vibrate        by remember { mutableStateOf(prefs.getBoolean("vibrate", true)) }
    var sound          by remember { mutableStateOf(prefs.getBoolean("sound", true)) }

    fun save() {
        prefs.edit()
            .putBoolean("alert_camera",    alertCamera)
            .putBoolean("alert_mic",       alertMic)
            .putBoolean("alert_location",  alertLocation)
            .putBoolean("background_only", bgOnly)
            .putBoolean("quiet_hours",     quietHours)
            .putBoolean("vibrate",         vibrate)
            .putBoolean("sound",           sound)
            .apply()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Configure your privacy monitoring", fontSize = 13.sp, color = TextMuted)

        Spacer(Modifier.height(20.dp))

        // ── Privacy Controls ──
        SettingsSection(title = "PRIVACY CONTROLS") {
            SettingsPermissionRow("📊", "Usage Access",
                "Required for full behavior analysis",
                granted = (ctx.getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager)
                    .checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(), ctx.packageName) == android.app.AppOpsManager.MODE_ALLOWED,
                onClick = { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            )
            HorizontalDivider(color = BgCardAlt, modifier = Modifier.padding(vertical = 4.dp))
            SettingsPermissionRow("🔔", "Notification Access",
                "Required to show privacy alerts",
                granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                    ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED else true,
                onClick = { ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Alert Triggers ──
        SettingsSection(title = "ALERT ME WHEN THESE ACTIVATE") {
            SettingsToggleRow("📷", "Camera", "Alert on camera access", alertCamera) {
                alertCamera = it; save()
            }
            SettingsToggleRow("🎤", "Microphone", "Alert on mic access", alertMic) {
                alertMic = it; save()
            }
            SettingsToggleRow("📍", "Location", "Alert on location access", alertLocation) {
                alertLocation = it; save()
            }
            SettingsToggleRow("📱", "Background only", "Only alert when app is in background", bgOnly) {
                bgOnly = it; save()
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Notifications ──
        SettingsSection(title = "NOTIFICATIONS") {
            SettingsToggleRow("📳", "Vibrate", "Vibrate on alerts", vibrate) { vibrate = it; save() }
            SettingsToggleRow("🔊", "Sound",   "Sound on alerts",   sound)   { sound   = it; save() }
            SettingsToggleRow("🌙", "Quiet Hours", "No alerts 11pm–7am", quietHours) { quietHours = it; save() }
        }

        Spacer(Modifier.height(16.dp))

        // ── Scan Controls ──
        SettingsSection(title = "SCAN") {
            Button(
                onClick = { viewModel.reset(); viewModel.startScan() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🔍  Run Full Scan Now", color = BgPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── About ──
        SettingsSection(title = "ABOUT") {
            InfoRow("App Version",  "1.0.0 — HackNdroid 2.0")
            InfoRow("Team",        "PaneerParathe")
            InfoRow("Built for",   "Hackathon Project")
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, fontSize = 10.sp, color = TextMuted, letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(4.dp)) { content() }
    }
}

@Composable
private fun SettingsToggleRow(
    emoji: String, title: String, subtitle: String,
    checked: Boolean, onToggle: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BgPrimary,
                checkedTrackColor = NeonGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = BgCardAlt
            )
        )
    }
}

@Composable
private fun SettingsPermissionRow(
    emoji: String, title: String, subtitle: String,
    granted: Boolean, onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (granted) NeonGreen.copy(alpha = 0.15f) else NeonRed.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                if (granted) "Granted" else "Not Granted",
                fontSize = 11.sp,
                color = if (granted) NeonGreen else NeonRed,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}