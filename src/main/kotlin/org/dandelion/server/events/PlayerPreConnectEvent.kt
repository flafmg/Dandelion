package org.dandelion.server.events

import org.dandelion.server.events.model.Cancellable
import org.dandelion.server.events.model.Event

class PlayerPreConnectEvent(
    val protocolVersion: Byte,
    val userName: String,
    val verificationKey: String,
    val unused: Byte,
    override var isCancelled: Boolean = false,
) : Event, Cancellable
