package com.sentinelx.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.sentinelx.shared.RawAppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScanner(private val context: Context) {

    suspend fun getInstalledApps(): List<RawAppData> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        // GET_PERMISSIONS flag retrieves package permissions
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

        packages.mapNotNull { packageInfo ->
            // Use safe calls (?.) because applicationInfo can technically be null
            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null

            val appName = appInfo.loadLabel(pm).toString()
            val icon = appInfo.loadIcon(pm)

            val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            val grantedPermissions = mutableListOf<String>()
            packageInfo.requestedPermissionsFlags?.let { flags ->
                requestedPermissions.forEachIndexed { index, perm ->
                    if ((flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                        grantedPermissions.add(perm)
                    }
                }
            }

            RawAppData(
                packageName = packageInfo.packageName,
                appName = appName,
                icon = icon,
                permissions = requestedPermissions,
                grantedPermissions = grantedPermissions
            )
        }
    }
}