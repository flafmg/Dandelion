package org.dandelion.classic.packets.server

import io.netty.channel.Channel
import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter

class UpdateUserType(
    val userType: Byte
) : Packet() {
    override val id: Byte = 0x0f
    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(userType)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}

