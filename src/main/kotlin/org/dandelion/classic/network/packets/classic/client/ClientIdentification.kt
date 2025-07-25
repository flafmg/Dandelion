package org.dandelion.classic.network.packets.classic.client

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketReader
import org.dandelion.classic.entity.player.Players

class ClientIdentification: Packet() {
    override val id: Byte = 0x00
    override val size = 131;

    var protocolVersion: Byte = 0x0
    var userName: String = ""
    var verificationKey: String = ""
    var unused: Byte = 0x0

    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)

        protocolVersion = reader.readByte()
        userName = reader.readString()
        verificationKey = reader.readString()
        unused = reader.readByte()
    }

    override fun resolve(channel: Channel) {
        Players.handlePreConnection(this, channel)
    }
}