package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.Players
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerExtAddEntity2(
    val entityId: Byte,
    val inGameName: String,
    val skinName: String,
    val spawnX: Float,
    val spawnY: Float,
    val spawnZ: Float,
    val spawnYaw: Byte,
    val spawnPitch: Byte,
) : Packet() {
    override val id: Byte = 0x21
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(entityId)
        writer.writeString(inGameName)
        writer.writeString(skinName)
        if (Players.supports(channel, "ExtEntityPositions")) {
            writer.writeFloat(spawnX)
            writer.writeFloat(spawnY)
            writer.writeFloat(spawnZ)
        } else {
            writer.writeFShort(spawnX)
            writer.writeFShort(spawnY)
            writer.writeFShort(spawnZ)
        }
        writer.writeByte(spawnYaw)
        writer.writeByte(spawnPitch)
        return writer.toByteArray()
    }
}
