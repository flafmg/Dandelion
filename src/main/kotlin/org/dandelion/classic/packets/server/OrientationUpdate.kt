package org.dandelion.classic.server.packets.server

import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketWriter
import io.netty.channel.Channel

class OrientationUpdate(
    val playerId: Byte,
    val yaw: Byte,
    val pitch: Byte
) : Packet() {
    override val id: Byte = 0x0b
    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeSByte(playerId)
        writer.writeByte(yaw)
        writer.writeByte(pitch)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
        sendNetty(channel)
    }
}

