package com.sentinelx.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val SPLASH      = "splash"
    const val PERMISSION  = "permission"
    const val SCAN        = "scan"
    const val DASHBOARD   = "dashboard"
    const val DETAIL      = "detail/{packageName}"
    const val REPORTS     = "reports"
    const val SETTINGS    = "settings"

    fun detail(pkg: String) = "detail/${URLEncoder.encode(pkg, "UTF-8")}"
}

@Composable
fun SentinelXNavGraph(
    navController: NavHostController,
    viewModel: ScanViewModel
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                navController.navigate(Routes.PERMISSION) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }

        composable(Routes.PERMISSION) {
            PermissionScreen(
                viewModel = viewModel,
                onPermissionGranted = {
                    navController.navigate(Routes.SCAN) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SCAN) {
            ScanScreen(
                viewModel = viewModel,
                onScanComplete = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DASHBOARD) {
            MainShell(
                viewModel = viewModel,
                navController = navController,
                startTab = Routes.DASHBOARD
            )
        }

        composable(Routes.REPORTS) {
            MainShell(
                viewModel = viewModel,
                navController = navController,
                startTab = Routes.REPORTS
            )
        }

        composable(Routes.SETTINGS) {
            MainShell(
                viewModel = viewModel,
                navController = navController,
                startTab = Routes.SETTINGS
            )
        }

        composable(Routes.DETAIL) { back ->
            val pkg = URLDecoder.decode(back.arguments?.getString("packageName") ?: "", "UTF-8")
            AppDetailScreen(packageName = pkg, viewModel = viewModel, navController = navController)
        }
    }
}