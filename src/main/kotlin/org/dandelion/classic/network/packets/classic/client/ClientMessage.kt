package org.dandelion.classic.network.packets.classic.client

import io.netty.channel.Channel
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketReader
import org.dandelion.classic.util.Utils

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
        val player = Players.find(channel)
        player?.handleSendMessageAs(this)
    }
}
