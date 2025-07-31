package org.dandelion.classic.network.packets.cpe.client

import io.netty.channel.Channel
import org.dandelion.classic.network.PacketRegistry
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketReader

class ClientCustomBlockLevel : Packet() {
    override val id: Byte = 0x13
    override val size = 2
    override val isCpe: Boolean = true

    var supportLevel: Byte = 0x00
    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)
        supportLevel = reader.readByte()
    }

    override fun resolve(channel: Channel) {
    }
}