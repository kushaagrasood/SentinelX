package com.sentinelx.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinelx.ui.theme.*

@Composable
fun PermissionScreen(
    viewModel: ScanViewModel,
    onPermissionGranted: () -> Unit
) {
    val hasPerm by viewModel.hasUsagePermission.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(hasPerm) {
        if (hasPerm) onPermissionGranted()
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
        .animateFloat(0.7f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "p")

    Box(
        Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔒", fontSize = 72.sp, modifier = Modifier.alpha(pulse.value))

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Permission Required",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "SentinelX needs Usage Access to analyze app behavior.",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "📊" to "Track which apps use your mic and camera",
                        "📍" to "Detect background location access",
                        "🔍" to "Identify suspicious app behavior"
                    ).forEach { (emoji, text) ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(text, fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Grant Permission", color = BgPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = onPermissionGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now", color = TextMuted, fontSize = 14.sp)
            }
        }
    }
}