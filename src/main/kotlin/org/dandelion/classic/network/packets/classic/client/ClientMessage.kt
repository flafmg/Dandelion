package org.dandelion.classic.network.packets.classic.client

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketReader
import org.dandelion.classic.entity.player.Players

class ClientMessage : Packet() {
    override val id: Byte = 0x0d
    override val size: Int = 66

    var playerId: Byte = 0x00
    var message: String = ""


    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)

        playerId = reader.readByte()
        message = reader.readString()
    }

    override fun resolve(channel: Channel) {
        val player = Players.find(channel)
        player?.sendMessageAs(message)
    }
}