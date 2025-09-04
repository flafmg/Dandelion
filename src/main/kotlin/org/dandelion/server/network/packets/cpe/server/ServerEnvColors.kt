package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerEnvColors(
    val variable: Byte,
    val red: Short,
    val green: Short,
    val blue: Short,
) : Packet() {
    override val id: Byte = 0x19
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(variable)
        writer.writeShort(red)
        writer.writeShort(green)
        writer.writeShort(blue)
        return writer.toByteArray()
    }
}
