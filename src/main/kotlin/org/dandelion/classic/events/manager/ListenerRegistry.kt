package org.dandelion.classic.events.manager

import org.dandelion.classic.events.model.Event
import org.dandelion.classic.events.model.EventListener

/**
 * ListenerRegistry manages the registration and unregistration of event listeners and their handlers.
 * It stores handlers mapped by event type and ensures handlers are sorted by priority.
 */
object ListenerRegistry {
    /**
     * Map of event types to their registered handler information.
     */
    internal val handlers = mutableMapOf<Class<out Event>, MutableList<EventHandlerInfo>>()

    /**
     * Registers an event listener and processes its event handler methods.
     *
     * @param listener The event listener to register.
     */
    fun register(listener: EventListener){
        val processed = EventProcessor.processListener(listener)
        if(processed == null)
            return;
        for(info in processed) {
            handlers.computeIfAbsent(info.eventType) {mutableListOf()}.add(info)
        }

        handlers.forEach { (_, list) ->
            list.sortBy { it.priority.ordinal }
        }
    }
    /**
     * Unregisters an event listener, removing all its handlers.
     *
     * @param listener The event listener to unregister.
     */
    fun unregister(listener: EventListener){
        for(list in handlers.values){
            list.removeIf{it.listener == listener}
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
