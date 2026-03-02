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
    // 🔥 THE KILLER FEATURE: Hidden Spyware Detector 🔥
    suspend fun getHiddenSpywareApps(): List<RawAppData> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val hiddenApps = mutableListOf<RawAppData>()

        for (packageInfo in packages) {
            val appInfo = packageInfo.applicationInfo ?: continue

            // System apps ko ignore karo (kyunki wo safe hote hain)
            if ((appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue
            }

            // Check karo ki kya is app ka koi 'Launcher Icon' hai?
            val intent = pm.getLaunchIntentForPackage(packageInfo.packageName)
            val isHidden = (intent == null) // Agar null hai, matlab app chupa hua hai!

            val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()

            // Agar app hidden hai AUR uske paas dangerous permissions hain, toh wo Spyware ho sakta hai!
            val hasDangerousPerms = requestedPermissions.any {
                it.contains("CAMERA") || it.contains("RECORD_AUDIO") || it.contains("FINE_LOCATION") || it.contains("READ_SMS")
            }

            if (isHidden && hasDangerousPerms) {
                val appName = appInfo.loadLabel(pm).toString()
                val icon = appInfo.loadIcon(pm)

                hiddenApps.add(
                    RawAppData(
                        packageName = packageInfo.packageName,
                        appName = "$appName ⚠️ (HIDDEN THREAT)",
                        icon = icon,
                        permissions = requestedPermissions,
                        grantedPermissions = requestedPermissions // Simplified for this check
                    )
                )
            }
        }

        hiddenApps // This will return all the hidden apps
    }
}