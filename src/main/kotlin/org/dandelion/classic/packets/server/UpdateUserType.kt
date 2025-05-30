package org.dandelion.classic.server.packets.server

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketWriter

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
        if (!PacketEventManager.fireSend(this, channel)) return
        sendNetty(channel)
    }
}

