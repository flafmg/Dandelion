package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerCustomBlockLevel(val supportLevel: Byte) : Packet() {
    override val id: Byte = 0x13
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(supportLevel)
        return writer.toByteArray()
    }
}
