package org.dandelion.classic.server.packets.server

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketWriter

class ServerSetBlock(
    val x: Short,
    val y: Short,
    val z: Short,
    val blockType: Byte
) : Packet() {
    override val id: Byte = 0x06
    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeShort(x)
        writer.writeShort(y)
        writer.writeShort(z)
        writer.writeByte(blockType)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
        if (!PacketEventManager.fireSend(this, channel)) return
        sendNetty(channel)
    }
}

