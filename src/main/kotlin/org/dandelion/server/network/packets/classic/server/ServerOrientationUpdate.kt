package org.dandelion.server.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerOrientationUpdate(
    val playerId: Byte,
    val yaw: Float,
    val pitch: Float,
) : Packet() {
    override val id: Byte = 0x0B

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        writer.writeAngleByte(yaw)
        writer.writePitchByte(pitch)
        return writer.toByteArray()
    }
}
