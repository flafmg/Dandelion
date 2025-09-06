package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.Players
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerSetInventoryOrder(val order: UShort, val blockId: UShort) :
    Packet() {
    override val id: Byte = 0x2C
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        if (Players.supports(channel, "ExtendedBlocks")) {
            writer.writeUShort(order)
            writer.writeUShort(blockId)
        } else {
            writer.writeByte((order and 0xFFu).toByte())
            writer.writeByte((blockId and 0xFFu).toByte())
        }
        return writer.toByteArray()
    }
}
