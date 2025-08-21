package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

/** Packet to remove a selection cuboid from the client */
class ServerRemoveSelection(private val selectionId: Byte) : Packet() {

    override val id: Byte = 0x1B
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(selectionId)
        return writer.toByteArray()
    }
}
