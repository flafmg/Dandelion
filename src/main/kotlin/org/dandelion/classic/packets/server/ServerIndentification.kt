package org.dandelion.classic.packets.server

import org.dandelion.classic.Server
import org.dandelion.classic.packets.model.Packet
import org.dandelion.classic.packets.stream.PacketWriter
import io.netty.channel.Channel
import org.dandelion.classic.data.config.manager.ServerConfigManager

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
       
        sendNetty(channel)
    }
}

