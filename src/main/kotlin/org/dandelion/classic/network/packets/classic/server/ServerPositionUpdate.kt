package org.dandelion.classic.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerPositionUpdate(
    val playerId: Byte,
    val changeInX: Float,
    val changeInY: Float,
    val changeInZ: Float,
) : Packet() {
    override val id: Byte = 0x0A

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        writer.writeFByte(changeInX)
        writer.writeFByte(changeInY)
        writer.writeFByte(changeInZ)
        return writer.toByteArray()
    }
}
