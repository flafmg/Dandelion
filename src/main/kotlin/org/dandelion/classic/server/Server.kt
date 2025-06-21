package org.dandelion.classic.server

import org.dandelion.classic.level.LevelManager
import org.dandelion.classic.level.generator.GeneratorRegistry
import org.dandelion.classic.network.ConnectionManager
import org.dandelion.classic.network.PacketFactory

object Server {
    private var running = false;

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
        if(running) return;

        PacketFactory.init()
        ConnectionManager.init()
        GeneratorRegistry.init()
        LevelManager.init()


    }
    fun shutDown(){
        if(!running) return;

        PacketFactory.shutDown()
        ConnectionManager.shutDown()
        GeneratorRegistry.shutDown()
        LevelManager.shutDown()
    }

    fun restart(){
        shutDown()
        init()
    }

    fun isRunning(): Boolean{
        return running
    }

}
fun main(){
    Server.init()
}