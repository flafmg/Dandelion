package org.dandelion.classic.packets.client

import org.dandelion.classic.data.player.manager.PlayerManager
import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketReader
import io.netty.channel.Channel

class ClientIndentification : Packet() {
    override val id: Byte = 0x00;
    override val size = 131

    var protocolVersion: Byte = 0x0
    var username: String = ""
    var verificationKey: String = ""
    var unused: Byte = 0x0
    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)
        protocolVersion = reader.readByte();
        username = reader.readString()
        verificationKey = reader.readString()
        unused = reader.readByte()
    }

    override fun resolve(channel: Channel) {

        PlayerManager.tryConnect(channel, this@ClientIndentification)
    }
}
