package com.sentinelx.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.sentinelx.ui.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }
    val nudge = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { alpha.animateTo(1f, tween(700)) }
            launch { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 180f)) }
        }
        delay(600)
        repeat(3) {
            nudge.animateTo(6f, tween(80))
            nudge.animateTo(-6f, tween(80))
        }
        nudge.animateTo(0f, tween(80))
        delay(500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .offset(x = nudge.value.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🛡️", fontSize = 80.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "SentinelX",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreen,
                modifier = Modifier.alpha(alpha.value),
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Privacy Protection Active",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.alpha(alpha.value),
                letterSpacing = 1.sp
            )
        }

        val dotAlpha = rememberInfiniteTransition(label = "dot")
            .animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "da")

        Text(
            text = "● ● ●",
            fontSize = 12.sp,
            color = NeonGreen.copy(alpha = dotAlpha.value),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            letterSpacing = 6.sp
        )
    }
}