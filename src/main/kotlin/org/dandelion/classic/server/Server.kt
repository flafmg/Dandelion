package org.dandelion.classic.server

import org.dandelion.classic.network.ConnectionManager
import org.dandelion.classic.network.PacketFactory
import org.dandelion.classic.network.handler.ConnectionHandler

object Server {
    private var isRunning = false;

    val dandelionVersion = "0.1a"
    val serverSoftware = "Dandelion $dandelionVersion"

    val port = 25565
    var name = "hi mom!"
    var motd = "love you :3"

    internal fun init(){
        if(isRunning) return;

        ConnectionManager.init()
        PacketFactory.init()
    }
    fun shutDown(){
        if(!isRunning) return;
    }

    fun restart(){
        shutDown()
        init()
    }


}
fun main(){
    Server.init()
}