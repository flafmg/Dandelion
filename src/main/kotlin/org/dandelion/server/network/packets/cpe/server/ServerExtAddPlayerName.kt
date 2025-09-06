package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerExtAddPlayerName(
    val nameId: Short,
    val playerName: String,
    val listName: String,
    val groupName: String,
    val groupRank: Byte,
) : Packet() {
    override val id: Byte = 0x16
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeShort(nameId)
        writer.writeString(playerName)
        writer.writeString(listName)
        writer.writeString(groupName)
        writer.writeByte(groupRank)
        return writer.toByteArray()
    }
}
