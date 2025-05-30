package org.dandelion.classic.packets.server

import io.netty.channel.Channel
import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter

class ServerMessage(
    val playerId: Byte,
    val message: String
) : Packet() {
    override val id: Byte = 0x0d
    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeSByte(playerId)
        writer.writeString(message)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}

