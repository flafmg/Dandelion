package org.dandelion.classic.server.packets.server

import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketWriter
import io.netty.channel.Channel
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager

class Ping : Packet() {
    override val id: Byte = 0x01
    override fun encode(): ByteArray {
        return byteArrayOf(id)
    }
    override fun resolve(channel: Channel) {
        if (!PacketEventManager.fireSend(this, channel)) return
        sendNetty(channel)
    }
}

