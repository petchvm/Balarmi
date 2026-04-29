package com.balarmi.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide live snapshot of the charging monitor, published by ChargingMonitorService
 * and consumed by SettingsScreen's hero card.
 */
object MonitorState {
    private val _state = MutableStateFlow(MonitorSnapshot(pct = -1, isCharging = false))
    val state: StateFlow<MonitorSnapshot> = _state.asStateFlow()

    fun update(pct: Int, isCharging: Boolean) {
        _state.value = MonitorSnapshot(pct, isCharging)
    }

    fun reset() {
        _state.value = MonitorSnapshot(pct = -1, isCharging = false)
    }
}

data class MonitorSnapshot(val pct: Int, val isCharging: Boolean)
