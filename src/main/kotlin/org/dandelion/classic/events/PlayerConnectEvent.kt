package org.dandelion.classic.events

import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.events.model.Event

class PlayerConnectEvent(val player: Player) : Event
