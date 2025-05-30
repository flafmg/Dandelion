package org.dandelion.classic.server.packets.server

import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketWriter
import io.netty.channel.Channel
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager

class LevelFinalize(
    private val xSize: Short,
    private val ySize: Short,
    private val zSize: Short
) : Packet() {
    override val id: Byte = 0x04

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeShort(xSize)
        writer.writeShort(ySize)
        writer.writeShort(zSize)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
        if (!PacketEventManager.fireSend(this, channel)) return
        sendNetty(channel)
    }
}

