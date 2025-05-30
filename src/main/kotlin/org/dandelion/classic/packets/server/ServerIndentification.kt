package org.dandelion.classic.server.packets.server

import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.server.Server
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketWriter
import io.netty.channel.Channel
import org.dandelion.classic.server.config.manager.ServerConfigManager
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager

class ServerIndentification : Packet() {
    override val id: Byte = 0x00

    private val config = ServerConfigManager.serverConfig.get()
    private val protocolVersion: Byte = Server.getProtocolVersion()
    private val serverName: String = config.serverSettings.name
    private val serverMotd: String = config.serverSettings.motd
    private val userType: Byte = 0x00

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(protocolVersion)
        writer.writeString(serverName)
        writer.writeString(serverMotd)
        writer.writeByte(userType)

        return writer.toByteArray();
    }

    override fun resolve(channel: Channel) {
        if (!PacketEventManager.fireSend(this, channel)) return
        sendNetty(channel)
    }
}

