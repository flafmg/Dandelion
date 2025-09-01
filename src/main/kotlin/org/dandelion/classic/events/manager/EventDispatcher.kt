package org.dandelion.classic.events.manager

import org.dandelion.classic.events.model.Cancellable
import org.dandelion.classic.events.model.Event
import org.dandelion.classic.server.Console

object EventDispatcher {
    fun dispatch(event: Event) {
        val handlers = ListenerRegistry.getApplicableHandlers(event)

        for (handler in handlers) {
            if (event is Cancellable && event.isCancelled) {
                continue
            }
            try {
                handler.method.invoke(handler.listener, event)
            } catch (e: Exception) {
                Console.errLog(
                    "Could not invoke event handler method in '${handler.listener::class.java.name}': ${e.message}"
                )
            }
        }
    }
}
