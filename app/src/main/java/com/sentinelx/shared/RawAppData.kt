package com.sentinelx.shared

import android.graphics.drawable.Drawable

data class RawAppData(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val permissions: List<String>,
    val grantedPermissions: List<String>,
    val firstInstallTime: Long,       // ms timestamp
    val lastUpdateTime: Long          // ms timestamp — used for "recently updated" flag
)