package com.sentinelx.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinelx.ui.theme.*

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onScanComplete: () -> Unit
) {
    val state by viewModel.scanState.collectAsState()

    LaunchedEffect(Unit) {
        if (state is ScanState.Idle) viewModel.startScan()
    }

    LaunchedEffect(state) {
        if (state is ScanState.Done) onScanComplete()
    }

    val rotation = rememberInfiniteTransition(label = "spin")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "r")

    Box(
        Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Spinning shield
            Text("🛡️", fontSize = 64.sp, modifier = Modifier.rotate(rotation.value))

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Analyzing Apps",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            val subtitle = when (val s = state) {
                is ScanState.Scanning -> s.currentApp
                else -> "Checking permissions and behavior patterns"
            }
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(Modifier.height(40.dp))

            // Progress bar
            val progress = when (val s = state) {
                is ScanState.Scanning -> s.progress / 100f
                is ScanState.Done     -> 1f
                else                  -> 0f
            }
            val animated by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(400),
                label = "prog"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = NeonGreen,
                    trackColor = BgCard,
                )

                Spacer(Modifier.height(12.dp))

                val progText = when (val s = state) {
                    is ScanState.Scanning -> "${(s.progress)}% analyzed"
                    is ScanState.Done     -> "${s.apps.size} apps scanned ✓"
                    else                  -> "Starting…"
                }
                Text(
                    text = progText,
                    fontSize = 13.sp,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            if (state is ScanState.Error) {
                Spacer(Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = (state as ScanState.Error).message,
                        color = NeonRed,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.startScan() }) {
                    Text("Retry")
                }
            }
        }
    }
}