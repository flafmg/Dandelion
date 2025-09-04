package org.dandelion.server.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerDespawnPlayer(val playerId: Byte) : Packet() {
    override val id: Byte = 0x0C

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(playerId)
        return writer.toByteArray()
    }
}
