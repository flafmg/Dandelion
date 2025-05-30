package org.dandelion.classic.packets.server

import io.netty.channel.Channel
import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter

class PositionAndOrientationUpdate(
    val playerId: Byte,
    val dx: Float,
    val dy: Float,
    val dz: Float,
    val yaw: Byte,
    val pitch: Byte
) : Packet() {
    override val id: Byte = 0x09
    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeSByte(playerId)
        writer.writeFByte(dx)
        writer.writeFByte(dy)
        writer.writeFByte(dz)
        writer.writeByte(yaw)
        writer.writeByte(pitch)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}
