package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerHoldThis(val blockToHold: UShort, val preventChange: Byte) :
    Packet() {
    override val id: Byte = 0x14
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        if (PlayerRegistry.supports(channel, "ExtendedBlocks")) {
            writer.writeUShort(blockToHold)
        } else {
            writer.writeByte((blockToHold and 0xFFu).toByte())
        }
        writer.writeByte(preventChange)
        return writer.toByteArray()
    }
}
