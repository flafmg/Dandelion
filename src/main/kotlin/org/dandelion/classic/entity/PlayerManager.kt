package org.dandelion.classic.entity

import io.netty.channel.Channel
import org.dandelion.classic.level.Level
import org.dandelion.classic.level.LevelManager
import org.dandelion.classic.network.packets.classic.client.ClientIdentification
import org.dandelion.classic.network.packets.classic.server.ServerDisconnectPlayer
import org.dandelion.classic.network.packets.classic.server.ServerIdentification
import org.dandelion.classic.server.Server

object PlayerManager {

    internal fun notifyPlayerJoin(player: Player){
        broadcast("${player.name} joined the server")
    }
    internal fun notifyPlayerLeft(player: Player){
        broadcast("${player.name} left the server")
    }
    internal fun notifyPlayerLevelJoin(player: Player, level: Level){
        broadcast("${player.name} joined level ${level.id}")
    }
    internal fun notifyPlayerLeaveLevel(player: Player, level: Level){
        broadcast("${player.name} left level ${level.id}")
    }

    internal fun disconnectPlayer(channel: Channel) {
        val player = getPlayerByChannel(channel) ?: return
        val level = player.level ?: return

        level.removeEntity(player)
        notifyPlayerLeft(player)
    }

    internal fun preConnectPlayer(client: ClientIdentification, channel: Channel){
        val player = Player(channel, "Unknown", client.userName)

        //in the future we fire a "preConnectPlayerevent

        player.levelId = LevelManager.defaultLevel
        ServerIdentification().send(player)
        tryConnectPlayer(player)
    }
    private fun tryConnectPlayer(player: Player){
        if(getPlayerByChannel(player.channel) != null || getAllPlayers().any() {it.name.equals(player.name)}){
            ServerDisconnectPlayer("You're already connected to this server").send(player)
            return
        }
        if(getOnlinePlayerCount() >=  Server.maxPlayers){
            ServerDisconnectPlayer("The server is full").send(player)
            return
        }
        val joinLevel = LevelManager.getDefaultJoinLevel()
        if(joinLevel == null){
            ServerDisconnectPlayer("Default level not available").send(player)
            return
        }

        player.sendToLevel(joinLevel)
        notifyPlayerJoin(player)
    }

    fun getPlayerByChannel(channel: Channel): Player? = 
        LevelManager.getAllPlayers().find { it.channel == channel }
    
    fun getPlayerByName(name: String): Player? =
        LevelManager.getAllPlayers().find { it.name.equals(name, ignoreCase = true) }
    
    fun getAllPlayers(): List<Player> = 
        LevelManager.getAllPlayers()
    
    fun getOnlinePlayerCount(): Int = 
        LevelManager.getOnlinePlayerCount()

    fun broadcast(message: String, id: Byte = 0x00) = 
        LevelManager.getAllPlayers().forEach { it.sendMessage(message, id) }
    
    fun kickAll(reason: String = "you have been kicked") = 
        LevelManager.getAllPlayers().forEach { it.kick(reason) }

    fun kickPlayer(name: String, reason: String = "You have been kicked") {
        getPlayerByName(name)?.kick(reason)
    }
    fun kickPlayer(player: Player, reason: String = "You have been kicked") {
        player.kick(reason)
    }
    
    fun banPlayer(name: String, reason: String = "You have been banned") {
        getPlayerByName(name)?.ban(reason)
    }
    fun banPlayer(player: Player, reason: String = "You have been banned") {
        player.ban(reason)
    }
}
