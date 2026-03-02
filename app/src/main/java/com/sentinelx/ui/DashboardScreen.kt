package com.sentinelx.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sentinelx.shared.AppInfo
import com.sentinelx.ui.theme.*

@Composable
fun DashboardScreen(viewModel: ScanViewModel, navController: NavController) {
    val state by viewModel.scanState.collectAsState()
    val apps  = (state as? ScanState.Done)?.apps  ?: emptyList()
    val summary = (state as? ScanState.Done)?.summary

    val deviceScore = if (apps.isNotEmpty())
        apps.take(10).map { it.riskScore }.average().toInt() else 0
    val deviceLevel = when {
        deviceScore >= 61 -> "HIGH RISK"
        deviceScore >= 31 -> "MODERATE RISK"
        else              -> "PROTECTED"
    }
    val deviceColor = riskColorByScore(deviceScore)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── Top bar ──
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("SentinelX", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                Text("Privacy Dashboard", fontSize = 12.sp, color = TextMuted)
            }
            IconButton(onClick = { viewModel.reset(); viewModel.startScan() }) {
                Text("🔄", fontSize = 20.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Hero Privacy Status Card ──
        val pulse = rememberInfiniteTransition(label = "hero")
            .animateFloat(0.95f, 1.0f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "h")

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(if (deviceScore >= 61) pulse.value else 1f),
            colors = CardDefaults.cardColors(containerColor = deviceColor.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, deviceColor.copy(alpha = 0.4f))
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Your Privacy Status", fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = deviceLevel,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = deviceColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Risk Score  $deviceScore / 100",
                    fontSize = 15.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { deviceScore / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = deviceColor,
                    trackColor = BgCardAlt
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${summary?.totalApps ?: apps.size} apps scanned",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Permission Status Cards ──
        val camApps = apps.count { it.sensitivePermissions.any { p -> p.contains("CAMERA") } }
        val micApps = apps.count { it.sensitivePermissions.any { p -> p.contains("RECORD_AUDIO") } }
        val locApps = apps.count { it.sensitivePermissions.any { p -> p.contains("LOCATION") } }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PermStatusCard("📷", "Camera",    camApps, Modifier.weight(1f))
            PermStatusCard("🎤", "Microphone", micApps, Modifier.weight(1f))
            PermStatusCard("📍", "Location",   locApps, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // ── Risk Summary Row ──
        if (summary != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RiskCountCard("🔴", "${summary.highCount}",   "High Risk",  NeonRed,    Modifier.weight(1f))
                RiskCountCard("🟠", "${summary.mediumCount}", "Medium",     NeonOrange, Modifier.weight(1f))
                RiskCountCard("🟢", "${summary.lowCount}",    "Safe",       NeonGreen,  Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Suspicious Apps ──
        val risky = apps.filter { it.riskLevel == "HIGH" }.take(10)
        if (risky.isNotEmpty()) {
            Text(
                "⚠️  Needs Attention",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            risky.forEach { app ->
                AppRiskRow(app = app, onClick = { navController.navigate(Routes.detail(app.packageName)) })
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── All Apps ──
        Text(
            "All Apps  (${apps.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        apps.forEach { app ->
            AppRiskRow(app = app, onClick = { navController.navigate(Routes.detail(app.packageName)) })
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PermStatusCard(emoji: String, label: String, count: Int, modifier: Modifier = Modifier) {
    val color = when {
        count >= 10 -> NeonRed
        count >= 4  -> NeonOrange
        else        -> NeonGreen
    }
    val status = when {
        count >= 10 -> "Frequent"
        count >= 4  -> "Occasional"
        else        -> "Safe"
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = TextMuted)
            Spacer(Modifier.height(4.dp))
            Text(status, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RiskCountCard(
    emoji: String, count: String, label: String,
    color: Color, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
fun AppRiskRow(app: AppInfo, onClick: () -> Unit) {
    val color = riskColor(app.riskLevel)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon placeholder circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BgCardAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.appName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    text = if (app.sensitivePermissions.isNotEmpty())
                        "${app.sensitivePermissions.size} sensitive access points"
                    else "No sensitive access",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${app.riskScore}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = app.riskLevel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}