package org.dandelion.classic.events.model
interface Cancellable {
    var isCancelled: Boolean

    fun cancel() {
        isCancelled = true
    }
}
