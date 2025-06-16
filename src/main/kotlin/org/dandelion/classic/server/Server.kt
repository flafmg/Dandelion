package org.dandelion.classic.server

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

    internal fun init(){
        if(isRunning) return;

        PacketFactory.init()
        ConnectionManager.init()
    }
    fun shutDown(){
        if(!isRunning) return;

        ConnectionManager.shutDown()
        PacketFactory.shutDown()
    }

    fun restart(){
        shutDown()
        init()
    }


}
fun main(){
    Server.init()
}