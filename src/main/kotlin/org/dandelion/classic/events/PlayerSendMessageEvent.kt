package org.dandelion.classic.events

import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.events.model.Cancellable
import org.dandelion.classic.events.model.Event

class PlayerSendMessageEvent(
    val player: Player,
    val message: String,
    override var isCancelled: Boolean = false,
) : Event, Cancellable
