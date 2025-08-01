package org.dandelion.classic.events

import org.dandelion.classic.events.model.Cancellable
import org.dandelion.classic.events.model.Event

class PlayerPreConnectEvent(
    val protocolVersion: Byte,
    val userName: String,
    val verificationKey: String,
    val unused: Byte,
    override var isCancelled: Boolean = false
) : Event, Cancellable