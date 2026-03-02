package com.sentinelx.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sentinelx.shared.AppTrustProfile
import com.sentinelx.shared.InstallSource
import com.sentinelx.shared.PermissionRisk
import com.sentinelx.shared.exportSummary
import com.sentinelx.shared.toReadableDuration
import com.sentinelx.ui.theme.*
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    viewModel: ScanViewModel,
    navController: NavController
) {
    val state = viewModel.scanState.collectAsState().value
    val app   = (state as? ScanState.Done)?.apps?.find { it.packageName == packageName }
    val ctx   = LocalContext.current

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = { Text(app?.appName ?: "App Detail", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        }
    ) { padding ->
        if (app == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonGreen)
            }
            return@Scaffold
        }

        val scoreColor = riskColorByScore(app.riskScore)

        // Resolve installer
        val installer = remember {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    ctx.packageManager.getInstallSourceInfo(app.packageName).installingPackageName
                else @Suppress("DEPRECATION") ctx.packageManager.getInstallerPackageName(app.packageName)
            } catch (e: Exception) { null }
        }
        val installSource = AppTrustProfile.resolveInstallSource(installer)

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Hero card ──
            Card(
                colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(BgCardAlt),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(app.appName.take(1).uppercase(), fontSize = 22.sp,
                                fontWeight = FontWeight.Bold, color = NeonGreen)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.appName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(app.packageName, fontSize = 11.sp, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${app.riskScore}", fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold, color = scoreColor)
                            Text("/ 100", fontSize = 12.sp, color = TextMuted)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Risk level badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = scoreColor.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = app.riskLevel,
                            color = scoreColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(app.riskExplanation, fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Score breakdown ──
            DetailSection(title = "📊 Risk Breakdown") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Extract scores from explanation string as fallback display
                    ScoreChip("Permissions", app.sensitivePermissions.size * 5, NeonRed, Modifier.weight(1f))
                    ScoreChip("Behavior",    if (app.permissionUsageDurations.isNotEmpty()) 40 else 20, NeonOrange, Modifier.weight(1f))
                    ScoreChip("Reputation",  if (installSource == InstallSource.SIDELOADED) 40 else 15, NeonPurple, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Install source ──
            DetailSection(title = "🏪 Install Source") {
                val sourceColor = when (installSource) {
                    InstallSource.PLAY_STORE, InstallSource.AMAZON_STORE,
                    InstallSource.SAMSUNG_STORE -> NeonGreen
                    InstallSource.SIDELOADED    -> NeonRed
                    else                        -> NeonOrange
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("●", color = sourceColor, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(installSource.displayName, color = TextPrimary, fontSize = 14.sp)
                }
                if (installSource == InstallSource.SIDELOADED) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚠️ Sideloaded apps bypass Play Store security checks.",
                        fontSize = 12.sp, color = NeonRed, lineHeight = 18.sp
                    )
                }
            }

            // ── Network ──
            if (app.networkUsageToday > 0) {
                Spacer(Modifier.height(12.dp))
                DetailSection(title = "📶 Network Activity") {
                    Text(
                        "Used ${app.networkUsageToday / 1024}KB of data in the last 24 hours",
                        fontSize = 13.sp, color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Permissions ──
            DetailSection(title = "🔐 Sensitive Access (${app.sensitivePermissions.size})") {
                if (app.sensitivePermissions.isEmpty()) {
                    Text("✅ No sensitive permissions detected.", fontSize = 13.sp, color = NeonGreen)
                } else {
                    app.sensitivePermissions.forEach { perm ->
                        val risk = PermissionRisk.forPermission(perm)
                        val usageMs = app.permissionUsageDurations[perm] ?: 0L
                        PermissionItem(
                            displayName = risk?.displayName ?: perm.substringAfterLast(".").replace("_", " "),
                            danger      = risk?.dangerLevel?.name ?: "UNKNOWN",
                            explanation = risk?.explanation ?: "",
                            usage       = if (usageMs > 0) usageMs.toReadableDuration() else null
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Action buttons ──
            Button(
                onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${app.packageName}"))
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BgCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("⚙️  Manage App Permissions", color = NeonGreen, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    ctx.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, app.exportSummary())
                                .putExtra(Intent.EXTRA_SUBJECT, "SentinelX: ${app.appName}"),
                            "Share Report"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
            ) {
                Text("📤  Share App Report", color = NeonGreen)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = TextSecondary, modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

@Composable
private fun ScoreChip(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
private fun PermissionItem(displayName: String, danger: String, explanation: String, usage: String?) {
    val color = when (danger) {
        "CRITICAL"  -> NeonRed
        "DANGEROUS" -> NeonOrange
        else        -> NeonGreen
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("●", color = color, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text(displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                Card(
                    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(danger, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                }
            }
            if (explanation.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(explanation, fontSize = 12.sp, color = TextMuted, lineHeight = 18.sp)
            }
            if (usage != null) {
                Spacer(Modifier.height(4.dp))
                Text("⏱ Used: $usage today", fontSize = 11.sp, color = NeonOrange)
            }
        }
    }
}