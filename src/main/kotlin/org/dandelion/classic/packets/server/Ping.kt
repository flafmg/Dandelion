package org.dandelion.classic.packets.server

import org.dandelion.classic.packets.model.Packet
import io.netty.channel.Channel

class Ping : Packet() {
    override val id: Byte = 0x01
    override fun encode(): ByteArray {
        return byteArrayOf(id)
    }
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}

