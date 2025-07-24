package org.dandelion.classic.events

import org.dandelion.classic.events.model.Cancellable
import org.dandelion.classic.events.model.Event

class HeartbeatSendEvent (
    var heartbeat: String,
    override var isCancelled: Boolean = false
) : Event, Cancellable