package org.dandelion.server.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.server.entity.player.Players
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter
import org.dandelion.server.util.Utils

class ServerDisconnectPlayer(val reason: String) : Packet() {
    override val id: Byte = 0x0E

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        val player = Players.find(channel)
        val supportsFullCp437 = player?.supports("FullCP437") ?: false

        writer.writeByte(id)
        writer.writeStringAsBytes(Utils.convertToCp437WithFallback(reason, supportsFullCp437))
        return writer.toByteArray()
    }
}