package org.dandelion.classic.events

import org.dandelion.classic.events.model.Cancellable
import org.dandelion.classic.events.model.Event
import org.dandelion.classic.level.Level
import org.dandelion.classic.entity.player.Player

class PlayerChangeLevel (
    val player: Player,
    val from: Level,
    val to: Level,
    override var isCancelled: Boolean = false
) : Event, Cancellable