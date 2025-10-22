package com.hritwik.avoid.core

import android.app.Service
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.reflect.KClass

object ServiceEventBus {
    sealed class Event {
        data class Start(val service: KClass<out Service>, val id: String? = null) : Event()
        data class Pause(val service: KClass<out Service>, val id: String? = null) : Event()
        data class Error(
            val service: KClass<out Service>,
            val id: String? = null,
            val throwable: Throwable
        ) : Event()
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun post(event: Event) {
        _events.tryEmit(event)
    }
}

