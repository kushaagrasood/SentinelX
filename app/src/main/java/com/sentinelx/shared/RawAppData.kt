package com.sentinelx.shared

import android.graphics.drawable.Drawable

data class RawAppData(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val permissions: List<String>,
    val grantedPermissions: List<String>
)