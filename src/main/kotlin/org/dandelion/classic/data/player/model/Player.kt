package org.dandelion.classic.data.player.model

import org.dandelion.classic.data.player.manager.PlayerManager
import org.dandelion.classic.data.level.model.Level
import org.dandelion.classic.commands.model.CommandExecutor
import io.netty.channel.Channel
import org.dandelion.classic.data.level.manager.LevelManager
import org.dandelion.classic.packets.server.*
import java.util.zip.GZIPOutputStream

data class Player(
    val channel: Channel,

    val protocol: Byte,
    var playerID: Byte,

    var isConnected: Boolean = false,

    val userName: String = "none",

    var levelId: String = "",

    var posX: Float = 0.0f,
    var posY: Float = 0.0f,
    var posZ: Float = 0.0f,
    var yaw: Float = 0.0f,
    var pitch: Float = 0.0f,

    var isOp: Boolean = false,
    var permissions: HashMap<String, Boolean> = hashMapOf("" to true),
) : CommandExecutor {
    fun grantOp(op: Boolean){
        isOp = op;
        if(isOp){
            UpdateUserType(0x64.toByte());
        }else{
            UpdateUserType(0x00.toByte());
        }
    }
    fun kick(reason: String = "Kicked by an operator") {
        DisconnectPlayer(reason).resolve(channel)
        PlayerManager.playerDisconnect(levelId, playerID)
        channel.close()
    }
    //man i love regex :3
    private fun recolorMessage(message: String): String {
        return message.replace(Regex("%([0-9a-fA-F])")) { matchResult ->
            "&${matchResult.groupValues[1].lowercase()}"
        }
    }
    //now the message gets splited if its longer than 64 chars (keeping track of the last color code)
    fun sendMessage(message: String, playerID: Byte = 0x00.toByte()) {
        val recolored = recolorMessage(message)
        var lastColor = ""
        val colorRegex = Regex("&([0-9a-f])")

        recolored.chunked(64).forEachIndexed { idx, chunk ->
            val chunkWithColor = if (idx == 0) chunk else lastColor + chunk

            ServerMessage(playerID, chunkWithColor).resolve(channel)

            colorRegex.findAll(chunk).lastOrNull()?.let {
                lastColor = it.value
            }
        }
    }
    fun teleport(posX: Float, posY: Float, posZ: Float, yaw: Float = 0f, pitch: Float = 0f) {
        this.posX = posX
        this.posY = posY
        this.posZ = posZ
        this.yaw = yaw
        this.pitch = pitch

        PlayerManager.sendSetPositionAndOrientation(
            levelId,
            playerID,
            posX,
            posY,
            posZ,
            yaw.toInt().toByte(),
            pitch.toInt().toByte()
        )
        SetPositionAndOrientation(
            0xff.toByte(),
            posX,
            posY,
            posZ,
            yaw.toInt().toByte(),
            pitch.toInt().toByte()
        ).resolve(channel)

    }
    fun sendLevelData(level: Level) {

        LevelInitialize().resolve(channel)
        val totalLength = level.blocks.size
        val prefixedData = ByteArray(4 + totalLength)
        prefixedData[0] = (totalLength shr 24).toByte()
        prefixedData[1] = (totalLength shr 16).toByte()
        prefixedData[2] = (totalLength shr 8).toByte()
        prefixedData[3] = totalLength.toByte()

        System.arraycopy(level.blocks, 0, prefixedData, 4, totalLength)

        val compressed = java.io.ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzip ->
                gzip.write(prefixedData)
            }
            baos.toByteArray()
        }

        val chunkSize = 1024
        for (i in compressed.indices step chunkSize) {
            val remaining = compressed.size - i
            val len = remaining.coerceAtMost(chunkSize)
            val chunk = ByteArray(len)
            System.arraycopy(compressed, i, chunk, 0, len)
            val percent = ((i + len).toFloat() / compressed.size * 100f).toInt().coerceAtMost(100).toByte()
            LevelDataChunk(len.toShort(), chunk, percent).resolve(channel)
        }

        LevelFinalize(level.sizeX, level.sizeY, level.sizeZ).resolve(channel)
        teleport(level.spawnX.toFloat(), level.spawnY.toFloat(), level.spawnZ.toFloat())
    }

    fun setPermission(permission: String, value: Boolean) {
        if (value) {
            permissions[permission] = true
        } else {
            permissions.remove(permission)
        }
    }

    fun changeLevel(newLevel: Level) {
        val newId = newLevel.getFirstAvailableId() ?: run {
            sendMessage("This level is full.")
            return
        }

        val oldLevel = LevelManager.getLevel(levelId)
        if (oldLevel != null) {
            oldLevel.getPlayers().filter { it.playerID != playerID }.forEach { other ->
                DespawnPlayer(playerID).resolve(other.channel)
            }
            oldLevel.removePlayer(playerID)
        }

        playerID = newId
        newLevel.players[newId.toInt() and 0xFF] = this
        levelId = newLevel.id

        grantOp(isOp)

        sendLevelData(newLevel)
        PlayerManager.sendSpawnPlayer(newLevel.id, this)
        teleport(newLevel.spawnX, newLevel.spawnY, newLevel.spawnZ)
        PlayerManager.announceLevelChange(this, newLevel)
    }

    override fun sendMessage(message: String) {
        this.sendMessage(message, 0x00.toByte())
    }

    override fun getName(): String = userName
    override fun hasPermission(permission: String): Boolean = isOp || permissions[permission] == true
    override fun isConsole(): Boolean = false
}
