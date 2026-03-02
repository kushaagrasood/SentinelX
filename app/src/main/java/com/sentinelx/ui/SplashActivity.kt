package com.sentinelx.ui

import com.sentinelx.R
import android.content.Intent
import android.os.Bundle
import android.view.animation.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val shield   = findViewById<ImageView>(R.id.splashShield)
        val glow     = findViewById<android.view.View>(R.id.glowCircle)
        val appName  = findViewById<TextView>(R.id.splashAppName)
        val subtitle = findViewById<TextView>(R.id.splashSubtitle)
        val divider  = findViewById<android.view.View>(R.id.splashDivider)
        val credit   = findViewById<TextView>(R.id.splashCredit)

        // Glow: fade in slowly
        glow.startAnimation(AlphaAnimation(0f, 1f).apply {
            duration = 1000
            fillAfter = true
        })

        // Shield: scale in with overshoot bounce
        shield.startAnimation(AnimationSet(true).apply {
            addAnimation(ScaleAnimation(0f, 1f, 0f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f).apply {
                duration = 650
                interpolator = OvershootInterpolator(2f)
            })
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 400 })
            fillAfter = true
        })

        // App name: slide up + fade
        appName.startAnimation(AnimationSet(true).apply {
            addAnimation(TranslateAnimation(0f, 0f, 50f, 0f).apply {
                duration = 500
                interpolator = DecelerateInterpolator(2f)
            })
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 500 })
            startOffset = 550
            fillAfter = true
        })

        // Subtitle: slide up + fade
        subtitle.startAnimation(AnimationSet(true).apply {
            addAnimation(TranslateAnimation(0f, 0f, 30f, 0f).apply {
                duration = 400
                interpolator = DecelerateInterpolator(2f)
            })
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 400 })
            startOffset = 800
            fillAfter = true
        })

        // Divider: expand from center
        divider.startAnimation(ScaleAnimation(0f, 1f, 1f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 500
            startOffset = 1000
            interpolator = DecelerateInterpolator()
            fillAfter = true
        })

        // Credit: fade in last
        credit.startAnimation(AlphaAnimation(0f, 1f).apply {
            duration = 600
            startOffset = 1300
            fillAfter = true
        })

        // Navigate to MainActivity
        CoroutineScope(Dispatchers.Main).launch {
            delay(2800)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}