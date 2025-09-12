package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerSetHotbar(val blockId: UShort, val hotbarIndex: Byte) : Packet() {
    override val id: Byte = 0x2D
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        if (PlayerRegistry.supports(channel, "ExtendedBlocks")) {
            writer.writeUShort(blockId)
        } else {
            writer.writeByte((blockId and 0xFFu).toByte())
        }
        writer.writeByte(hotbarIndex)
        return writer.toByteArray()
    }
}
