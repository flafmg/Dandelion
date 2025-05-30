package org.dandelion.classic.server.packets.client

import org.dandelion.classic.server.data.player.manager.PlayerManager
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketReader
import io.netty.channel.Channel
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager
import org.dandelion.classic.server.util.Logger

class ClientMessage : Packet() {
    override val id: Byte = 0x0d
    override val size = 66

    var unused: Byte = 0xFF.toByte()
    var message: String = ""

    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)
        unused = reader.readByte()
        val available = data.size - 2
        val maxLen = minOf(64, available)
        message = if (maxLen > 0) reader.readString(maxLen) else ""
    }

    override fun resolve(channel: Channel) {
        if (!PacketEventManager.fireReceive(this, channel)) return
        val player = PlayerManager.getPlayerByChannel(channel) ?: return
        Logger.log("[${player.levelId}] ${player.userName} > $message")
        PlayerManager.sendPlayerMessage(message, player)
    }
}
