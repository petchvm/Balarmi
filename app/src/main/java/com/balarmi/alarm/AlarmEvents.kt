package com.balarmi.alarm

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide event bus for service → activity signalling.
 * The reverse direction (activity → service) goes via Service intents.
 */
object AlarmEvents {
    private val _events = MutableSharedFlow<AlarmEvent>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AlarmEvent> = _events.asSharedFlow()

    fun emit(event: AlarmEvent) {
        _events.tryEmit(event)
    }
}

sealed class AlarmEvent {
    /** Service has stopped the alarm; AlarmActivity should finish. */
    data object Stopped : AlarmEvent()
}
