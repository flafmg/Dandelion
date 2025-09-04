package org.dandelion.server.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.Players
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerSetPositionAndOrientation(
    val playerId: Byte,
    val x: Float,
    val y: Float,
    val z: Float,
    val yaw: Byte,
    val pitch: Byte,
) : Packet() {
    override val id: Byte = 0x08

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        if (Players.supports(channel, "ExtEntityPositions")) {
            writer.writeFloat(x)
            writer.writeFloat(y)
            writer.writeFloat(z)
        } else {
            writer.writeFShort(x)
            writer.writeFShort(y)
            writer.writeFShort(z)
        }
        writer.writeByte(yaw)
        writer.writeByte(pitch)
        return writer.toByteArray()
    }
}
