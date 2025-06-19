package org.dandelion.classic.server

import org.dandelion.classic.level.LevelManager
import org.dandelion.classic.level.generator.GeneratorRegistry
import org.dandelion.classic.network.ConnectionManager
import org.dandelion.classic.network.PacketFactory
import org.dandelion.classic.network.handler.ConnectionHandler

object Server {
    private var isRunning = false;

    // software info
    val dandelionVersion = "0.1a"
    val serverSoftware = "Dandelion $dandelionVersion"

    // server info
    val isCpe = false
    val port = 25565
    var name = "hi mom!"
    var motd = "love you :3"

    var maxPlayers = 255

    internal fun init(){
        if(isRunning) return;

        PacketFactory.init()
        ConnectionManager.init()
        GeneratorRegistry.init()
        LevelManager.init()


    }
    fun shutDown(){
        if(!isRunning) return;

        PacketFactory.shutDown()
        ConnectionManager.shutDown()
        GeneratorRegistry.shutDown()
        LevelManager.shutDown()
    }

    fun restart(){
        shutDown()
        init()
    }


}
fun main(){
    Server.init()
}