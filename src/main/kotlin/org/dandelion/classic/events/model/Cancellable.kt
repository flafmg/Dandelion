package org.dandelion.classic.events.model

/**
 * Cancellable is an interface for events that can be cancelled.
 */
interface Cancellable {
    /**
     * Indicates whether the event is cancelled.
     */
    var isCancelled: Boolean

    /**
     * Cancels the event by setting isCancelled to true.
     */
    fun cancel(){
        isCancelled = true
    }
}