package org.dandelion.server.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter
import org.dandelion.server.util.Utils

class ServerMessage(val playerId: Byte, val message: String) : Packet() {
    override val id: Byte = 0x0D

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        val player = PlayerRegistry.find(channel)
        val supportsFullCp437 = player?.supports("FullCP437") ?: false

        writer.writeByte(id)
        writer.writeByte(playerId)
        writer.writeStringAsBytes(Utils.convertToCp437WithFallback(message, supportsFullCp437))
        return writer.toByteArray()
    }
}
