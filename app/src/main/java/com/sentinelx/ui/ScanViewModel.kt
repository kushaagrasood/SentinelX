package com.sentinelx.ui

import android.app.Application
import android.app.AppOpsManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sentinelx.logic.AppProcessor
import com.sentinelx.shared.AppInfo
import com.sentinelx.logic.RiskSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val progress: Int, val total: Int, val currentApp: String = "") : ScanState()
    data class Done(val apps: List<AppInfo>, val summary: RiskSummary, val scanMs: Long) : ScanState()
    data class Error(val message: String) : ScanState()
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    fun checkUsagePermission() {
        val ctx = getApplication<Application>()
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            ctx.packageName
        )
        _hasUsagePermission.value = (mode == AppOpsManager.MODE_ALLOWED)
    }

    fun startScan() {
        if (_scanState.value is ScanState.Scanning) return
        val ctx = getApplication<Application>()

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            // Fake progressive scan for UX
            _scanState.value = ScanState.Scanning(0, 100, "Initializing…")
            delay(300)
            _scanState.value = ScanState.Scanning(15, 100, "Reading installed apps…")
            delay(400)
            _scanState.value = ScanState.Scanning(30, 100, "Checking permissions…")

            val (apps, summary) = try {
                AppProcessor.processAppsWithContext(ctx)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error("Scan failed: ${e.message}")
                return@launch
            }

            val total = apps.size
            _scanState.value = ScanState.Scanning(50, total, "Analyzing behavior patterns…")
            delay(400)
            _scanState.value = ScanState.Scanning(70, total, "Calculating risk scores…")
            delay(300)
            _scanState.value = ScanState.Scanning(90, total, "Generating report…")
            delay(300)

            val scanMs = System.currentTimeMillis() - startTime
            AppProcessor.saveSession(ctx, com.sentinelx.shared.ScanSession.create(apps, scanMs))
            _scanState.value = ScanState.Done(apps, summary, scanMs)
        }
    }

    fun reset() { _scanState.value = ScanState.Idle }
}