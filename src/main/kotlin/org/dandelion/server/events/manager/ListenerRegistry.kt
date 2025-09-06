package org.dandelion.server.events.manager

import org.dandelion.server.events.model.Event
import org.dandelion.server.events.model.EventListener

object ListenerRegistry {
    internal val handlers =
        mutableMapOf<Class<out Event>, MutableList<EventHandlerInfo>>()

    fun register(listener: EventListener) {
        val processed = EventProcessor.processListener(listener)
        if (processed == null) return
        for (info in processed) {
            handlers
                .computeIfAbsent(info.eventType) { mutableListOf() }
                .add(info)
        }

        handlers.forEach { (_, list) -> list.sortBy { it.priority.ordinal } }
    }

    fun unregister(listener: EventListener) {
        for (list in handlers.values) {
            list.removeIf { it.listener == listener }
        }
    }

    internal fun getApplicableHandlers(event: Event): List<EventHandlerInfo> {
        val eventClass = event::class.java

        return handlers
            .filterKeys { it.isAssignableFrom(eventClass) }
            .values
            .flatten()
            .sortedBy { it.priority.ordinal }
    }
}
