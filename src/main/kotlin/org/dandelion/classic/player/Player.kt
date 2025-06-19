package org.dandelion.classic.player

import io.netty.channel.Channel
import kotlinx.coroutines.launch
import org.dandelion.classic.level.Level
import org.dandelion.classic.level.LevelManager
import org.dandelion.classic.network.packets.classic.server.*
import org.dandelion.classic.types.Block
import org.dandelion.classic.types.Position
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

data class Player(
    val channel: Channel,
    val client: String,
    val name: String,

    var levelId: String = "",
    var playerId: Byte = -1,

    val position: Position = Position(0f,0f,0f,0f,0f),
    var isOp: Boolean = false,
){
    var level: Level? = null
    
    fun sendMessage(message: String){
        sendMessage(message, 0xff.toByte())
    }
    fun sendMessage(message: String, id: Byte = 0xFF.toByte()){
        ServerMessage(id.toByte() ,message).send(channel)
    }
    fun kick(reason: String = "you have been kicked"){

    }
    fun ban(reason: String = "you have been banned from this server"){

    }
    fun setPosition(position: Position){
        setPosition(position.x, position.y, position.z, position.yaw, position.pitch)
    }
    fun setPosition(x: Int, y: Int, z: Int){
        setPosition(x.toFloat(), y.toFloat(), z.toFloat(), position.yaw, position.pitch)
    }
    fun setPosition(x: Float, y: Float, z: Float){
        setPosition(x, y, z, position.yaw, position.pitch)
    }
    fun setPosition(x: Float, y: Float, z: Float, yaw: Float, pitch: Float){
        position.set(x, y, z, yaw, pitch)

        sendSetPositionAndOrientation(x, y, z, yaw.toInt().toByte(), pitch.toInt().toByte())
        ServerSetPositionAndOrientation(-1 , x, y, z, yaw.toInt().toByte(), pitch.toInt().toByte()).send(channel)
    }
    fun setRotation(yaw: Float, pitch: Float){
        setPosition(position.x, position.y, position.z, yaw, pitch)
    }

    fun sendToLevel(level: Level){
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
            level.spawnPlayer(this@Player)

            ServerLevelFinalize(level.size.x, level.size.y, level.size.z).send(channel)

        }

    }

    fun sendSetPositionAndOrientation(x: Float, y: Float, z: Float, yaw: Byte, pitch: Byte) {
        level?.getPlayers()?.filter { it.playerId != playerId }?.forEach { player ->
            ServerSetPositionAndOrientation(playerId, x, y, z, yaw, pitch).send(player.channel)
        }
    }
    private fun sendPositionAndOrientationUpdate(dx: Float, dy: Float, dz: Float, yaw: Byte, pitch: Byte) {
        level?.getPlayers()?.filter { it.playerId != playerId }?.forEach { player ->
            ServerPositionAndOrientationUpdate(playerId, dx, dy, dz, yaw, pitch).send(player.channel)
        }
    }

    private fun sendPositionUpdate(dx: Float, dy: Float, dz: Float) {
        level?.getPlayers()?.filter { it.playerId != playerId }?.forEach { player ->
            ServerPositionUpdate(playerId, dx, dy, dz).send(player.channel)
        }
    }

    private fun sendOrientationUpdate(yaw: Byte, pitch: Byte) {
        level?.getPlayers()?.filter { it.playerId != playerId }?.forEach { player ->
            ServerOrientationUpdate(playerId, yaw, pitch).send(player.channel)
        }
    }
    internal fun updatePlayerPositionAndOrientation(newX: Float, newY: Float, newZ: Float, newYaw: Float, newPitch: Float, absolute: Boolean = false) {
        val dx = newX - position.x
        val dy = newY - position.y
        val dz = newZ - position.z

        val dyaw = newYaw - position.yaw
        val dpitch = newPitch - position.pitch

        val hasMoved = dx != 0f || dy != 0f || dz != 0f
        val hasRotated = dyaw != 0f || dpitch != 0f

        val maxRel = 3.96875f
        val minRel = -4.0f

        val useAbsolute = absolute || dx < minRel || dx > maxRel || dy < minRel || dy > maxRel || dz < minRel || dz > maxRel

        if (useAbsolute && (hasMoved || hasRotated)) {
            sendSetPositionAndOrientation(newX, newY, newZ, newYaw.toInt().toByte(), newPitch.toInt().toByte())
        } else if (hasMoved && hasRotated) {
            sendPositionAndOrientationUpdate(dx, dy, dz, newYaw.toInt().toByte(), newPitch.toInt().toByte())
        } else if (hasMoved) {
            sendPositionUpdate(dx, dy, dz)
        } else if (hasRotated) {
            sendOrientationUpdate(newYaw.toInt().toByte(), newPitch.toInt().toByte())
        }

        position.set(newX, newY, newZ, newYaw, newPitch)
    }
    internal fun sendMessageAsPlayer(message: String){
        level?.broadcast("&7$name: &f$message")
    }

    internal fun setBlockAsPlayer(player: Player, x: Short, y: Short, z: Short, block: Byte, mode: Byte){
        if(level == null) return

        val dx = player.position.x - x
        val dy = player.position.y - y
        val dz = player.position.z - z
        val distance = Math.sqrt((dx * dx + dy* dy + dz * dz).toDouble())

        val maxDist = 6.0 // in the future change this to be defined by level ( or player?)

        val finalBlockType = if (mode == 0x00.toByte()) Block.Air.id else block

        if(distance <= maxDist){
            level!!.setBlock(x, y, z, finalBlockType)
        }

        val trueBlock = level!!.getBlock(x, y, z)
        level!!.getPlayers().forEach{ player ->
            ServerSetBlock(x, y, z, trueBlock).send(player)
        }
    }
}