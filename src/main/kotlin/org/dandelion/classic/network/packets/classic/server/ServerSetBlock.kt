package org.dandelion.classic.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSetBlock(
    val x: Short,
    val y: Short,
    val z: Short,
    val blockType: UShort,
    val useExtendedBlocks: Boolean = true,
) : Packet() {
    override val id: Byte = 0x06

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeShort(x)
        writer.writeShort(y)
        writer.writeShort(z)
        if (Players.supports(channel, "ExtendedBlocks")) {
            writer.writeUShort(blockType)
        } else {
            writer.writeByte((blockType and 0xFFu).toByte())
        }
        return writer.toByteArray()
    }
}
