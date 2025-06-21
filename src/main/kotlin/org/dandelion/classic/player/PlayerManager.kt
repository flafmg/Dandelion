package org.dandelion.classic.player

import io.netty.channel.Channel
import org.dandelion.classic.level.Level
import org.dandelion.classic.level.LevelManager
import org.dandelion.classic.network.packets.classic.client.ClientIdentification
import org.dandelion.classic.network.packets.classic.server.ServerDisconnectPlayer
import org.dandelion.classic.network.packets.classic.server.ServerIdentification
import org.dandelion.classic.server.Console
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
        val level = LevelManager.getAllLevels().find { it.isPlayerInLevel(player) } ?: return

        level.removePlayer(player)
        notifyPlayerLeft(player)
    }

    internal fun preConnectPlayer(client: ClientIdentification, channel: Channel){
        val player = Player(channel, "Unknown", client.userName,)
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
        val joinLevel = LevelManager.getLevel(player.levelId)
        if(joinLevel == null){
            ServerDisconnectPlayer("You're already connected to this server").send(player)
            return
        }
        // in the future add ban check

        //here in the future call "playerConnectEvent"
        player.sendToLevel(joinLevel)
        notifyPlayerJoin(player)
    }


    fun getPlayerByChannel (channel: Channel): Player? = LevelManager.getAllLevels().flatMap { it.getPlayers() }.find { it.channel == channel }
    fun getAllPlayers(): List<Player> = LevelManager.getAllLevels().flatMap { it.getPlayers() }
    fun getOnlinePlayerCount(): Int = LevelManager.getAllLevels().sumOf { it.getPlayerCount() }

    fun broadcast(message: String, id: Byte = 0xFF.toByte()) = LevelManager.getAllLevels().forEach() {it.broadcast(message, id)}
    fun kickAll(reason: String = "you have been kicked") = LevelManager.getAllLevels().forEach() { it.kickAll(reason) }

}