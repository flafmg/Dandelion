package org.dandelion.classic.packets.server

import io.netty.channel.Channel
import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter

class SetPositionAndOrientation(
    val playerId: Byte,
    val x: Float,
    val y: Float,
    val z: Float,
    val yaw: Byte,
    val pitch: Byte
) : Packet() {
    override val id: Byte = 0x08
    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeSByte(playerId)
        writer.writeFShort(x)
        writer.writeFShort(y)
        writer.writeFShort(z)
        writer.writeByte(yaw)
        writer.writeByte(pitch)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}
