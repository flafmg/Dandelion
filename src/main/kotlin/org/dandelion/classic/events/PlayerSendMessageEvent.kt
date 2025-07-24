package org.dandelion.classic.events

import org.dandelion.classic.events.model.Cancellable
import org.dandelion.classic.events.model.Event
import org.dandelion.classic.player.Player

class PlayerSendMessageEvent(
    val player: Player,
    val message: String,
    override var isCancelled: Boolean = false,
) : Event, Cancellable