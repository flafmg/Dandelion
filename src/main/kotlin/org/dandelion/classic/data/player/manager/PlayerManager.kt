package org.dandelion.classic.data.player.manager

import org.dandelion.classic.data.level.manager.LevelManager
import org.dandelion.classic.data.player.model.Player
import org.dandelion.classic.packets.client.ClientIndentification
import io.netty.channel.Channel
import org.dandelion.classic.Console
import org.dandelion.classic.packets.server.*
import org.dandelion.classic.commands.manager.CommandRegistry
import org.dandelion.classic.data.config.manager.ServerConfigManager
import org.dandelion.classic.data.level.model.Level

object PlayerManager {
    fun getPlayerByChannel(channel: Channel): Player? {
        return LevelManager.getAllLevels().flatMap { it.getPlayers() }.find { it.channel == channel }
    }

    fun kickAllPlayers(reason: String = "Kicked by an operator") {
        LevelManager.getLevelPlayers(LevelManager.defaultLevel).forEach { p -> p.kick(reason = reason) }
    }

    fun sendSetPositionAndOrientation(levelId: String, playerId: Byte, x: Float, y: Float, z: Float, yaw: Byte, pitch: Byte) {
        LevelManager.getLevelPlayers(levelId).filter { it.playerID != playerId }.forEach { player ->
            SetPositionAndOrientation(playerId, x, y, z, yaw, pitch).resolve(player.channel)
        }
    }

    fun sendPositionAndOrientationUpdate(levelId: String, playerId: Byte, dx: Float, dy: Float, dz: Float, yaw: Byte, pitch: Byte) {
        LevelManager.getLevelPlayers(levelId).filter { it.playerID != playerId }.forEach { player ->
            PositionAndOrientationUpdate(playerId, dx, dy, dz, yaw, pitch).resolve(player.channel)
        }
    }

    fun sendPositionUpdate(levelId: String, playerId: Byte, dx: Float, dy: Float, dz: Float) {
        LevelManager.getLevelPlayers(levelId).filter { it.playerID != playerId }.forEach { player ->
            PositionUpdate(playerId, dx, dy, dz).resolve(player.channel)
        }
    }

    fun sendOrientationUpdate(levelId: String, playerId: Byte, yaw: Byte, pitch: Byte) {
        LevelManager.getLevelPlayers(levelId).filter { it.playerID != playerId }.forEach { player ->
            OrientationUpdate(playerId, yaw, pitch).resolve(player.channel)
        }
    }

    fun sendMessage(message: String, playerId: Byte = 0xff.toByte()) {
        LevelManager.getAllLevels().forEach { level ->
            LevelManager.sendMessageToLevel(level.id, message, playerId)
        }
    }

    fun sendPlayerMessage(message: String, player: Player) {
        if (message.startsWith(CommandRegistry.prefix)) {
            CommandRegistry.execute(message, player)
            return
        }
        val name = player.userName
        val format = "&f${name}: &7${message}"
        sendMessage(format)
    }

    fun announceLevelChange(player: Player, newLevel: Level) {
        sendMessage("&e${player.userName} &7has joined level &e'${newLevel.id}'")
    }

    fun announceDisconnect(player: Player) {
        sendMessage("&e${player.userName} &7has left the game")
    }

    fun tryConnect(channel: Channel, packet: ClientIndentification) {
        if (getPlayerByChannel(channel) != null){
            DisconnectPlayer("You are already connected to this server").resolve(channel)
            return;
        }
        if (getAllPlayers().any { it.userName.equals(packet.username, ignoreCase = true) }) {
            DisconnectPlayer("You are already connected to this server").resolve(channel)
            return;
        }
        if (ServerConfigManager.bansConfig.isBanned(packet.username)) {
            DisconnectPlayer("Youre Banned from this server").resolve(channel)
            return
        }
        val defaultLevel = LevelManager.getDefaultJoinLevel()
        if (defaultLevel == null) {
            DisconnectPlayer("The default level couldn't be found").resolve(channel)
            return
        }
        val availableId = defaultLevel.getFirstAvailableId()
        if (availableId == null) {
            DisconnectPlayer("This level is Full").resolve(channel)
            return
        }
        val player = Player(
            channel = channel,
            protocol = packet.protocolVersion,
            playerID = availableId,
            userName = packet.username,
            levelId = defaultLevel.id
        )
        if (ServerConfigManager.opsConfig.isOp(player.userName)) {
            player.grantOp(true)
        }
        ServerConfigManager.permissionsConfig.applyDefaultPermissions(player.userName)
        ServerConfigManager.permissionsConfig.getPermissions(player.userName).forEach {
            player.setPermission(it, true)
        }
        if (!defaultLevel.addPlayer(player)) {
            DisconnectPlayer("Couldn't connect to level").resolve(channel)
            return
        }
        ServerIndentification().resolve(player.channel)
        player.sendLevelData(defaultLevel)
        sendSpawnPlayer(player.levelId, player)
        Console.log("${player.userName} has joined level ${player.levelId}")
        announceLevelChange(player, defaultLevel)

        player.sendMessage("&7> &bHi!&f, this is &edandelion, &fa silly")
        player.sendMessage("&7> &fclassic server software im making for fun")
        player.sendMessage("&7> &fBy &aflaffymg! &fMade in &5Kotlin")
        player.sendMessage("&7> &bhttps://github.com/flafmg/dandelion ")
        player.sendMessage("> &fUse &d/commands &fto see available commands")
        player.sendMessage("> &fUse &d/level list &fto see available levels")
        player.sendMessage("> &fUse &d/level go <name> &fto teleport to a level")
    }

    fun sendSpawnPlayer(levelId: String, player: Player) {
        val players = LevelManager.getLevelPlayers(levelId)
        // spawn player to evryone
        players.filter { it.playerID != player.playerID }.forEach { other ->
            SpawnPlayer(
                playerId = player.playerID,
                playerName = player.userName,
                x = player.posX,
                y = player.posY,
                z = player.posZ,
                yaw = player.yaw.toInt().toByte(),
                pitch = player.pitch.toInt().toByte()
            ).resolve(other.channel)
        }
        // spawn everyone to player
        players.filter { it.playerID != player.playerID }.forEach { other ->
            SpawnPlayer(
                playerId = other.playerID,
                playerName = other.userName,
                x = other.posX,
                y = other.posY,
                z = other.posZ,
                yaw = other.yaw.toInt().toByte(),
                pitch = other.pitch.toInt().toByte()
            ).resolve(player.channel)
        }
    }

    fun playerDisconnect(levelId: String, playerID: Byte) {
        val level = LevelManager.getLevel(levelId) ?: return
        val player = level.getPlayerById(playerID) ?: return
        val players = level.getPlayers()
        players.filter { it.playerID != playerID }.forEach { p ->
            DespawnPlayer(playerID).resolve(p.channel)
        }
        level.removePlayer(playerID)
        try {
            player.channel.close()
        } catch (_: Exception) {}
        announceDisconnect(player)
        Console.log("${player.userName} has left the game")
    }

    fun count(levelId: String): Int = LevelManager.getLevelPlayers(levelId).size

    fun getOnlinePlayerCount(): Int {
        return LevelManager.getAllLevels().sumOf { it.getPlayers().size }
    }

    fun getAllPlayers(): List<Player> {
        return LevelManager.getAllLevels().flatMap { it.getPlayers() }
    }

    fun updatePlayerPositionAndOrientation(player: Player, newX: Float, newY: Float, newZ: Float, newYaw: Float, newPitch: Float, absolute: Boolean = false) {
        val dx = newX - player.posX
        val dy = newY - player.posY
        val dz = newZ - player.posZ

        val dyaw = newYaw - player.yaw
        val dpitch = newPitch - player.pitch

        val hasMoved = dx != 0f || dy != 0f || dz != 0f
        val hasRotated = dyaw != 0f || dpitch != 0f

        val maxRel = 3.96875f
        val minRel = -4.0f

        val useAbsolute = absolute || dx < minRel || dx > maxRel || dy < minRel || dy > maxRel || dz < minRel || dz > maxRel

        if (useAbsolute && (hasMoved || hasRotated)) {
            sendSetPositionAndOrientation(player.levelId, player.playerID, newX, newY, newZ, newYaw.toInt().toByte(), newPitch.toInt().toByte())
        } else if (hasMoved && hasRotated) {
            sendPositionAndOrientationUpdate(player.levelId, player.playerID, dx, dy, dz, newYaw.toInt().toByte(), newPitch.toInt().toByte())
        } else if (hasMoved) {
            sendPositionUpdate(player.levelId, player.playerID, dx, dy, dz)
        } else if (hasRotated) {
            sendOrientationUpdate(player.levelId, player.playerID, newYaw.toInt().toByte(), newPitch.toInt().toByte())
        }

        player.posX = newX
        player.posY = newY
        player.posZ = newZ
        player.yaw = newYaw
        player.pitch = newPitch
    }
}
