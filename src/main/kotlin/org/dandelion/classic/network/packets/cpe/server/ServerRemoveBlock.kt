package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerRemoveBlock(val blockId: UShort) : Packet() {
    override val id: Byte = 0x24
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        if (Players.supports(channel, "ExtendedBlocks")) {
            writer.writeUShort(blockId)
        } else {
            writer.writeByte(blockId.toByte())
        }
        return writer.toByteArray()
    }
}
