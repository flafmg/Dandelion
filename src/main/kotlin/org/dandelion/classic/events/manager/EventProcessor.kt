package org.dandelion.classic.events.manager

import org.dandelion.classic.events.annotations.EventHandler
import org.dandelion.classic.events.annotations.EventPriority
import org.dandelion.classic.events.model.Event
import org.dandelion.classic.events.model.EventListener
import java.lang.reflect.Method
import kotlin.jvm.java


internal data class EventHandlerInfo(
    val listener: EventListener,
    val method: Method,
    val priority: EventPriority,
    val eventType: Class<out Event>
)
internal object EventProcessor {
    fun processListener(listener: EventListener): List<EventHandlerInfo>?{
        return listener::class.java.methods
            .filter { isValidHandlerMethod(it) }
            .mapNotNull {createHandlerInfo(it, listener) }
    }

    private fun isValidHandlerMethod(method: Method): Boolean{
        if(!method.isAnnotationPresent(EventHandler::class.java))
            return false
        val params = method.parameterTypes
        return params.size == 1 && Event::class.java.isAssignableFrom(params[0])
    }
    private fun createHandlerInfo(method: Method, listener: EventListener): EventHandlerInfo?{
        val paramType = method.parameterTypes.first()
        @Suppress("UNCHECKED_CAST")
        val eventType = paramType as? Class<out Event> ?: return null

        val annotation = method.getAnnotation(EventHandler::class.java)
        return EventHandlerInfo(
            listener,
            method,
            annotation.priority,
            eventType
        )
    }
}