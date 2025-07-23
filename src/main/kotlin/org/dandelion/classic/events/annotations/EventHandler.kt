package org.dandelion.classic.events.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler(
    val priority: EventPriority = EventPriority.NORMAL
)


