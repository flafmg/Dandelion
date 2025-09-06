package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSetSpawnpoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val yaw: Float,
    val pitch: Float,
) : Packet() {
    override val id: Byte = 0x2E
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        if (Players.supports(channel, "ExtEntityPositions")) {
            writer.writeFloat(x)
            writer.writeFloat(y)
            writer.writeFloat(z)
        } else {
            writer.writeFShort(x)
            writer.writeFShort(y)
            writer.writeFShort(z)
        }
        writer.writeAngleByte(yaw)
        writer.writePitchByte(pitch)
        return writer.toByteArray()
    }
}
