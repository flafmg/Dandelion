package org.dandelion.classic.events

import org.dandelion.classic.events.model.Cancellable
import org.dandelion.classic.events.model.Event
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.types.Position

class PlayerMoveEvent(
    val player: Player,
    val from: Position,
    val to: Position,
    override var isCancelled: Boolean = false
) : Event, Cancellable