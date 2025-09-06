package org.dandelion.server.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter
import org.dandelion.server.server.data.ServerConfig

class ServerIdentification(
    val protocolVersion: Byte = 0x07,
    val serverName: String = ServerConfig.name,
    val serverMotd: String = ServerConfig.motd,
    val userType: Byte = 0x64,
) : Packet() {
    override val id: Byte = 0x00

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(protocolVersion)
        writer.writeString(serverName)
        writer.writeString(serverMotd)
        writer.writeByte(userType)
        return writer.toByteArray()
    }
}
