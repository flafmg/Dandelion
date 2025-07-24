package org.dandelion.classic.events

import org.dandelion.classic.blocks.model.Block
import org.dandelion.classic.level.Level
import org.dandelion.classic.player.Player
import org.dandelion.classic.types.Position

class PlayerBlockInteractionEvent (
    val player: Player,
    val oldBlock: Block,
    val newBlock: Block,
    val position: Position,
    val level: Level,
)