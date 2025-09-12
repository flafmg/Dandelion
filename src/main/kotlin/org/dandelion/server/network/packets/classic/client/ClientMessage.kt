package org.dandelion.server.network.packets.classic.client

import io.netty.channel.Channel
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketReader
import org.dandelion.server.util.Utils

class ClientMessage : Packet() {
    override val id: Byte = 0x0d
    override val size: Int = 66

    var messageType: Byte = 0x00
    var message: String = ""

    override fun decode(data: ByteArray, channel: Channel) {
        val reader = PacketReader(data)

        messageType = reader.readByte()
        val messageBytes = reader.readStringAsBytes()
        message = Utils.convertFromCp437(messageBytes)
    }

    override fun resolve(channel: Channel) {
        val player = PlayerRegistry.find(channel)
        player?.handleSendMessageAs(this)
    }
}
