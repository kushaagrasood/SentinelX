package com.sentinelx.data

import com.sentinelx.shared.MonitorEvent

object PrivacyMonitorService {
    private val events = mutableListOf<MonitorEvent>()

    fun getRecentEvents(limit: Int): List<MonitorEvent> {
        return events.takeLast(limit).reversed()
    }

    fun logEvent(event: MonitorEvent) {
        events.add(event)
    }
}