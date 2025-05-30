package org.dandelion.classic.packets.server

import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter
import io.netty.channel.Channel

class DespawnPlayer(
    val playerId: Byte
) : Packet() {
    override val id: Byte = 0x0c
    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeSByte(playerId)
        return writer.toByteArray()
    }
    override fun resolve(channel: Channel) {
       
        sendNetty(channel)
    }
}

