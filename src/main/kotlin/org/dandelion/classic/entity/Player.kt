package org.dandelion.classic.entity

import io.netty.channel.Channel
import kotlinx.coroutines.launch
import org.dandelion.classic.level.Level
import org.dandelion.classic.network.packets.classic.server.*
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

    var isOp: Boolean = false,
) : Entity(name, levelId, entityId, position) {

    fun sendMessage(message: String){
        sendMessage(message, 0x00)
    }
    
    fun sendMessage(message: String, id: Byte = 0x00){
        ServerMessage(id.toByte(), message).send(channel)
    }
    
    fun kick(reason: String = "you have been kicked"){
        ServerDisconnectPlayer(reason).send(channel)
        PlayerManager.disconnectPlayer(channel)
    }
    
    fun ban(reason: String = "you have been banned from this server"){
        // need to add logic
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
    
    override fun updateBlock(x: Short, y: Short, z: Short, block: Byte) {
        ServerSetBlock(x, y, z, block).send(channel)
    }
}