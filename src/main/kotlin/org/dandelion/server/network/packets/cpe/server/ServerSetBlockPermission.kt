package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.Players
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerSetBlockPermission(
    val blockType: UShort,
    val allowPlacement: Boolean,
    val allowDeletion: Boolean,
) : Packet() {
    override val id: Byte = 0x1C
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        if (Players.supports(channel, "ExtendedBlocks")) {
            writer.writeUShort(blockType)
        } else {
            writer.writeByte((blockType and 0xFFu).toByte())
        }
        writer.writeBoolean(allowPlacement)
        writer.writeBoolean(allowDeletion)
        return writer.toByteArray()
    }
}
