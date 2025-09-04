package org.dandelion.server.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerPing : Packet() {
    override val id: Byte = 0x01

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        return writer.toByteArray()
    }
}
