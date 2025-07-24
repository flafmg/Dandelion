package org.dandelion.classic.events

import org.dandelion.classic.events.model.Event
import org.dandelion.classic.player.Player

class PlayerConnectEvent (
    val player: Player
) : Event