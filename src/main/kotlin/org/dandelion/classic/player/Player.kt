package org.dandelion.classic.player

import io.netty.channel.Channel
import kotlinx.coroutines.launch
import org.dandelion.classic.commands.model.CommandExecutor
import org.dandelion.classic.events.OnPlayerMove
import org.dandelion.classic.events.manager.EventDispatcher
import org.dandelion.classic.level.Level
import org.dandelion.classic.network.packets.classic.server.*
import org.dandelion.classic.server.Console
import org.dandelion.classic.types.Position
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class Player(
    val channel: Channel,
    val client: String,
    name: String,
    levelId: String = "",
    entityId: Byte = -1,
    position: Position = Position(0f,0f,0f,0f,0f),
    val info: PlayerInfo = PlayerInfo.getOrCreate(name),

    override val permissions: List<String> = listOf(),
) : Entity(name, levelId, entityId, position), CommandExecutor {

    override fun sendMessage(message: String){
        sendMessage(message, 0x00)
    }
    
    fun sendMessage(message: String, id: Byte = 0x00){
        val messages = splitMessage(message)
        messages.forEach { msg ->
            ServerMessage(id, msg).send(channel)
        }
    }

    private fun splitMessage(message: String, maxLength: Int = 64): List<String> {
        if (message.length <= maxLength) {
            return listOf(message)
        }


        val result = mutableListOf<String>()
        var remaining = message
        var lastColorCode = ""

        while (remaining.length > maxLength) {
            var splitIndex = maxLength
            val lastSpaceIndex = remaining.substring(0, maxLength).lastIndexOf(' ')
            if (lastSpaceIndex > 0) {
                splitIndex = lastSpaceIndex
            }
            val currentPart = remaining.substring(0, splitIndex)

            val colorCodeRegex = "&[0-9a-fA-F]".toRegex()
            val colorMatches = colorCodeRegex.findAll(currentPart)
            if (colorMatches.any()) {
                lastColorCode = colorMatches.last().value
            }
            result.add(currentPart)
            remaining = if (lastSpaceIndex > 0) {
                remaining.substring(splitIndex + 1)
            } else {
                remaining.substring(maxLength)
            }
            if (lastColorCode.isNotEmpty() && remaining.isNotEmpty()) {
                if (!remaining.startsWith("&")) {
                    remaining = lastColorCode + remaining
                }
            }
        }

        if (remaining.isNotEmpty()) {
            result.add(remaining)
        }

        return result
    }
    
    fun kick(reason: String = "you have been kicked"){
        ServerDisconnectPlayer(reason).send(channel)
        Players.disconnect(channel)
    }
    
    fun ban(reason: String = "no reason gave"){
        info.banned = true
        info.banReason = reason
        kick("You're banned: $reason")
    }

    override fun setPosition(x: Float, y: Float, z: Float, yaw: Float, pitch: Float) {
        super.setPosition(x, y, z, yaw, pitch)
        ServerSetPositionAndOrientation(-1, x, y, z, yaw.toInt().toByte(), pitch.toInt().toByte()).send(channel)
    }

    override fun sendToLevel(level: Level){
        ServerLevelInitialize().send(channel)
        if (!level.trySetId(this)) {
            sendMessage("Level is full")
            return
        }
        this.level = level
        Players.notifyLevelJoin(this, level)
        kotlinx.coroutines.GlobalScope.launch{
            ServerLevelInitialize().send(channel)

            val totalLength = level.blocks.size
            val prefixedData = ByteArray(4 + totalLength)

            prefixedData[0] = (totalLength shr 24).toByte()
            prefixedData[1] = (totalLength shr 16).toByte()
            prefixedData[2] = (totalLength shr 8).toByte()
            prefixedData[3] = totalLength.toByte()

            System.arraycopy(level.blocks, 0, prefixedData, 4, totalLength)

            val compressed = ByteArrayOutputStream().use { baos ->
                GZIPOutputStream(baos).use { gzip ->
                    gzip.write(prefixedData)
                }
                baos.toByteArray()
            }

            val chunkSize = 1024
            for(i in compressed.indices step chunkSize){
                val remaining = compressed.size - i
                val len = remaining.coerceAtMost(chunkSize)
                val chunk = ByteArray(len)
                System.arraycopy(compressed, i, chunk, 0, len)
                val percent = ((i + len).toFloat() / compressed.size * 100f).toInt().coerceAtMost(100).toByte()
                ServerLevelDataChunk(len.toShort(), chunk, percent).send(channel)
            }

            setPosition(level.spawn)
            level.spawnPlayerInLevel(this@Player)

            ServerLevelFinalize(level.size.x, level.size.y, level.size.z).send(channel)
        }
    }

    override fun spawnEntityMutual(other: Entity) {
        this.spawnEntityFor(other)
        other.spawnEntityFor(this)
    }
    
    override fun despawnEntityMutual(other: Entity) {
        this.despawnEntityFor(other)
        other.despawnEntityFor(this)
    }
    override fun sendMessageAsEntity(message: String) {
        Console.log("[$levelId] $name: $message")
        if(message.startsWith("/")){
            sendCommand(message)
            return
        }
        level?.broadcast("$name: &7$message")
    }
    override fun updateBlock(x: Short, y: Short, z: Short, block: Byte) {
        ServerSetBlock(x, y, z, block).send(channel)
    }

    override fun updateEntityPositionAndOrientation(
        newX: Float,
        newY: Float,
        newZ: Float,
        newYaw: Float,
        newPitch: Float,
        absolute: Boolean
    ) {
        val newPosition = Position(newX, newY, newZ, newYaw, newPitch)
        if(this.position == newPosition){
            return
        }

        val event = OnPlayerMove(this, this.position, Position(newX, newY, newZ, newYaw, newPitch))
        EventDispatcher.dispatch(event)
        if(event.isCancelled){
            setPosition(position) //todo: add a internal signature to ignore the "teleport" event we will have in the future
        } else{
            super.updateEntityPositionAndOrientation(newX, newY, newZ, newYaw, newPitch, absolute)
        }
    }
}