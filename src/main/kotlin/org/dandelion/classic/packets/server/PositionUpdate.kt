package org.dandelion.classic.packets.server

import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter
import io.netty.channel.Channel

class PositionUpdate(
    val playerId: Byte,
    val dx: Float,
    val dy: Float,
    val dz: Float
) : Packet() {
    override val id: Byte = 0x0a
    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeSByte(playerId)
        writer.writeFByte(dx)
        writer.writeFByte(dy)
        writer.writeFByte(dz)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}
