package org.dandelion.server.events

import org.dandelion.server.events.model.Cancellable
import org.dandelion.server.events.model.Event

class HeartbeatSendEvent(
    var heartbeat: String,
    override var isCancelled: Boolean = false,
) : Event, Cancellable
