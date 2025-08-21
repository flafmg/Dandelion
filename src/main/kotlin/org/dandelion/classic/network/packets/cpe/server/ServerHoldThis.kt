package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerHoldThis(val blockToHold: UShort, val preventChange: Byte) :
    Packet() {
    override val id: Byte = 0x14
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        if (Players.supports(channel, "ExtendedBlocks")) {
            writer.writeUShort(blockToHold)
        } else {
            writer.writeByte((blockToHold and 0xFFu).toByte())
        }
        writer.writeByte(preventChange)
        return writer.toByteArray()
    }
}
