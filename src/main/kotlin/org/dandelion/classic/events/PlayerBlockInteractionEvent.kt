package org.dandelion.classic.events

import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.events.model.Cancellable
import org.dandelion.classic.events.model.Event
import org.dandelion.classic.level.Level
import org.dandelion.classic.types.Position

class PlayerBlockInteractionEvent(
    val player: Player,
    val oldBlock: Block,
    val newBlock: Block,
    val position: Position,
    val level: Level,
    override var isCancelled: Boolean = false,
) : Event, Cancellable
