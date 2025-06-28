package org.dandelion.classic.player

import io.netty.channel.Channel
import org.dandelion.classic.level.Level
import org.dandelion.classic.level.Levels
import org.dandelion.classic.network.packets.classic.client.ClientIdentification
import org.dandelion.classic.network.packets.classic.server.ServerDisconnectPlayer
import org.dandelion.classic.network.packets.classic.server.ServerIdentification
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.ServerInfo
import java.security.MessageDigest
import java.util.Date

//TODO: needs to be refactored at some point, do the approach goodly mentioned?
object Players {
    internal fun notifyJoin(player: Player){
        Console.log("${player.name} joined")
        broadcast("${player.name} joined the server")
    }
    internal fun notifyLeft(player: Player){
        Console.log("${player.name} left")
        broadcast("${player.name} left the server")
    }
    internal fun notifyLevelJoin(player: Player, level: Level){
        Console.log("${player.name} joined ${level.id}")
        broadcast("${player.name} joined level ${level.id}")
    }

    internal fun disconnect(channel: Channel) {
        val player = byChannel(channel) ?: return
        val level = player.level ?: return

        level.removeEntity(player)
        notifyLeft(player)
        player.info.lastSeen = Date()
        player.info.totalPlaytime += player.info.lastSeen.time - player.info.lastJoin.time
        player.info.save()
    }

    internal fun preConnect(client: ClientIdentification, channel: Channel){
        ServerIdentification().send(channel)

        if(client.protocolVersion != 0x07.toByte()){
            ServerDisconnectPlayer("You're using an invalid protocol version (${client.protocolVersion} but i expected 7)")
            return
        }
        if(ServerInfo.verifyUsers){
            val md = MessageDigest.getInstance("MD5")
            val computedHash = md.digest("${ServerInfo.salt}${client.userName}".toByteArray())
            val computedHashHex = computedHash.joinToString("") { "%02x".format(it) }
            if(client.verificationKey != computedHashHex){
                ServerDisconnectPlayer("You're not logged in").send(channel)
                return
            }
        }

        val player = Player(channel, "Unknown", client.userName)
        //in the future we fire a "preConnectPlayerevent

        player.levelId = Levels.defaultLevel

        tryConnect(player)
    }
    private fun tryConnect(player: Player){
        if(player.info.banned){
            ServerDisconnectPlayer("You're banned: ${player.info.banReason}").send(player)
            return
        }
        if(byChannel(player.channel) != null || all().any() { it.name == player.name }){
            ServerDisconnectPlayer("You're already connected to this server").send(player)
            return
        }
        if(count() >=  ServerInfo.maxPlayers){
            ServerDisconnectPlayer("The server is full").send(player)
            return
        }
        val joinLevel = Levels.getDefault()
        if(joinLevel == null){
            ServerDisconnectPlayer("Default level not available").send(player)
            return
        }

        player.info.lastJoin = Date()
        player.info.joinCount ++
        player.info.save()

        player.sendToLevel(joinLevel)
        notifyJoin(player)
    }

    fun byChannel(channel: Channel): Player? =
        Levels.players().find { it.channel == channel }

    fun byName(name: String): Player? =
        Levels.players().find { it.name.equals(name, ignoreCase = true) }

    fun all(): List<Player> =
        Levels.players()

    fun count(): Int =
        Levels.playerCount()

    fun broadcast(message: String, id: Byte = 0x00) =
        all().forEach { it.sendMessage(message, id) }

    fun kickAll(reason: String = "you have been kicked") =
        all().forEach { it.kick(reason) }

    fun kick(name: String, reason: String = "You have been kicked") {
        byName(name)?.kick(reason)
    }
    fun kick(player: Player, reason: String = "You have been kicked") {
        player.kick(reason)
    }
    
    fun ban(name: String, reason: String = "You have been banned") {
        byName(name)?.ban(reason)
    }
    fun ban(player: Player, reason: String = "You have been banned") {
        player.ban(reason)
    }
}
