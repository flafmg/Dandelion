package org.dandelion.classic.packets.server

import org.dandelion.classic.packets.model.Packet
import io.netty.channel.Channel

class LevelInitialize : Packet() {
    override val id: Byte = 0x02

    override fun encode(): ByteArray {
        return byteArrayOf(id)
    }

    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}

