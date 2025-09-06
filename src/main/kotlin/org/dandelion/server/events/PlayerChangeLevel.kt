package org.dandelion.server.events

import org.dandelion.server.entity.player.Player
import org.dandelion.server.events.model.Cancellable
import org.dandelion.server.events.model.Event
import org.dandelion.server.level.Level

class PlayerChangeLevel(
    val player: Player,
    val from: Level,
    val to: Level,
    override var isCancelled: Boolean = false,
) : Event, Cancellable
