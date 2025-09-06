package org.dandelion.server.events.model
interface Cancellable {
    var isCancelled: Boolean

    fun cancel() {
        isCancelled = true
    }
}
