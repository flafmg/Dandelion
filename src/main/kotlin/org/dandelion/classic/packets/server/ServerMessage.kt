package org.dandelion.classic.server.packets.server

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketWriter
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager

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
        if (!PacketEventManager.fireSend(this, channel)) return
        sendNetty(channel)
    }
}

