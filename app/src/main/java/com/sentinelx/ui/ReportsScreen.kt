package com.sentinelx.ui

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinelx.logic.AppProcessor
import com.sentinelx.ui.theme.*

@Composable
fun ReportsScreen(viewModel: ScanViewModel) {
    val state = viewModel.scanState.collectAsState().value
    val apps  = (state as? ScanState.Done)?.apps ?: emptyList()
    val ctx   = LocalContext.current

    val micApps  = apps.count { it.sensitivePermissions.any { p -> p.contains("RECORD_AUDIO") } }
    val camApps  = apps.count { it.sensitivePermissions.any { p -> p.contains("CAMERA") } }
    val locApps  = apps.count { it.sensitivePermissions.any { p -> p.contains("LOCATION") } }
    val smsApps  = apps.count { it.sensitivePermissions.any { p -> p.contains("SMS") } }
    val highRisk = apps.filter { it.riskLevel == "HIGH" }
    val medRisk  = apps.filter { it.riskLevel == "MEDIUM" }
    val lowRisk  = apps.filter { it.riskLevel == "LOW" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Privacy Report", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Today's findings from your device", fontSize = 13.sp, color = TextMuted)

        Spacer(Modifier.height(20.dp))

        // ── Today's findings ──
        Card(
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("TODAY'S FINDINGS", fontSize = 10.sp, color = TextMuted, letterSpacing = 1.sp)
                Spacer(Modifier.height(14.dp))
                FindingRow("🎤", "$micApps apps accessed your microphone")
                FindingRow("📷", "$camApps apps accessed your camera")
                FindingRow("📍", "$locApps apps tracked your location")
                FindingRow("💬", "$smsApps apps can read your messages")
                if (highRisk.isNotEmpty())
                    FindingRow("🚨", "${highRisk.size} suspicious app(s) detected")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Risk Summary ──
        Text("RISK SUMMARY", fontSize = 10.sp, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RiskSummaryCard("Safe Apps",       "${lowRisk.size}",  NeonGreen,  "🟢", Modifier.weight(1f))
            RiskSummaryCard("Needs Attention", "${medRisk.size}",  NeonOrange, "🟠", Modifier.weight(1f))
            RiskSummaryCard("High Risk",       "${highRisk.size}", NeonRed,    "🔴", Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))

        // ── High risk apps listed ──
        if (highRisk.isNotEmpty()) {
            Text("HIGH RISK APPS", fontSize = 10.sp, color = NeonRed, letterSpacing = 1.sp)
            Spacer(Modifier.height(10.dp))
            highRisk.forEach { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("${app.sensitivePermissions.size} sensitive access points",
                                fontSize = 12.sp, color = TextMuted)
                        }
                        Text("${app.riskScore}", fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, color = NeonRed)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Export ──
        Text("EXPORT", fontSize = 10.sp, color = TextMuted, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))

        listOf("TXT" to "📄", "JSON" to "🗂️").forEach { (format, emoji) ->
            OutlinedButton(
                onClick = {
                    val report = AppProcessor.generateReport(apps, emptyList())
                    val content = when (format) {
                        "JSON" -> reportToJson(report, apps)
                        else   -> report.toShareableText()
                    }
                    ctx.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, content)
                                .putExtra(Intent.EXTRA_SUBJECT, "SentinelX_Report"),
                            "Export as $format"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(vertical = 3.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f))
            ) {
                Text("$emoji  Export as $format", color = NeonGreen, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FindingRow(emoji: String, text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = TextSecondary)
    }
}

@Composable
private fun RiskSummaryCard(
    label: String, count: String, color: androidx.compose.ui.graphics.Color,
    emoji: String, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(count, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        }
    }
}

private fun reportToJson(report: com.sentinelx.shared.PrivacyReport, apps: List<com.sentinelx.shared.AppInfo>): String {
    val sb = StringBuilder()
    sb.appendLine("{")
    sb.appendLine("  \"generatedAt\": ${report.generatedAt},")
    sb.appendLine("  \"deviceRiskScore\": ${report.deviceRiskScore},")
    sb.appendLine("  \"deviceRiskLevel\": \"${report.deviceRiskLevel}\",")
    sb.appendLine("  \"totalAppsScanned\": ${report.totalAppsScanned},")
    sb.appendLine("  \"highRiskApps\": ${report.highRiskApps.size},")
    sb.appendLine("  \"apps\": [")
    apps.forEachIndexed { i, app ->
        sb.appendLine("    {")
        sb.appendLine("      \"name\": \"${app.appName}\",")
        sb.appendLine("      \"package\": \"${app.packageName}\",")
        sb.appendLine("      \"riskScore\": ${app.riskScore},")
        sb.appendLine("      \"riskLevel\": \"${app.riskLevel}\",")
        sb.appendLine("      \"sensitivePermissions\": ${app.sensitivePermissions.size}")
        sb.append("    }${if (i < apps.lastIndex) "," else ""}\n")
    }
    sb.appendLine("  ]")
    sb.appendLine("}")
    return sb.toString()
}