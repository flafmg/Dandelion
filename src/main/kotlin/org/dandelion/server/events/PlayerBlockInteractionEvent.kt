package org.dandelion.server.events

import org.dandelion.server.blocks.model.Block
import org.dandelion.server.entity.player.Player
import org.dandelion.server.events.model.Cancellable
import org.dandelion.server.events.model.Event
import org.dandelion.server.level.Level
import org.dandelion.server.types.Position

class PlayerBlockInteractionEvent(
    val player: Player,
    val oldBlock: Block,
    val newBlock: Block,
    val position: Position,
    val level: Level,
    override var isCancelled: Boolean = false,
) : Event, Cancellable
