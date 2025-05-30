package org.dandelion.classic.packets.server

import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter
import io.netty.channel.Channel

class DisconnectPlayer(
    val reason: String
) : Packet() {
    override val id: Byte = 0x0e
    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeString(reason)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}

