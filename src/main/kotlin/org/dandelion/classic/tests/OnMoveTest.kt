package org.dandelion.classic.tests

import org.dandelion.classic.events.OnPlayerMove
import org.dandelion.classic.events.annotations.EventHandler
import org.dandelion.classic.events.model.EventListener
import org.dandelion.classic.server.Console

class OnMoveTest: EventListener {

    @EventHandler
    fun onMove(event: OnPlayerMove){
        Console.infoLog("Event Received: moving from ${event.from.toString()} to ${event.to.toString()} for player ${event.player.name}")
        //event.cancel()
    }
}