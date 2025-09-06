package org.dandelion.server.events

import org.dandelion.server.entity.player.Player
import org.dandelion.server.events.model.Cancellable
import org.dandelion.server.events.model.Event
import org.dandelion.server.types.Position

class PlayerMoveEvent(
    val player: Player,
    val from: Position,
    val to: Position,
    override var isCancelled: Boolean = false,
) : Event, Cancellable
