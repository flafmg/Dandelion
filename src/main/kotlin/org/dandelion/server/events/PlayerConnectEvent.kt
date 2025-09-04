package org.dandelion.server.events

import org.dandelion.server.entity.player.Player
import org.dandelion.server.events.model.Event

class PlayerConnectEvent(val player: Player) : Event
