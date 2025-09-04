package org.dandelion.server.events

import org.dandelion.server.entity.player.Player
import org.dandelion.server.events.model.Cancellable
import org.dandelion.server.events.model.Event

class PlayerSendMessageEvent(
    val player: Player,
    val message: String,
    override var isCancelled: Boolean = false,
) : Event, Cancellable
