package org.dandelion.classic.server.packets.client

import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.server.data.player.manager.PlayerManager
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.server.ServerIndentification
import org.dandelion.classic.server.packets.stream.PacketReader
import org.dandelion.classic.server.util.Logger
import io.netty.channel.Channel
import org.dandelion.classic.server.Server
import kotlinx.coroutines.launch
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager

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
        if (!PacketEventManager.fireReceive(this, channel)) return
        PlayerManager.tryConnect(channel, this@ClientIndentification)
    }
}
