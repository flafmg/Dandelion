package org.dandelion.classic.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter
import org.dandelion.classic.server.ServerInfo

class ServerIdentification(
    val serverName: String = ServerInfo.name,
    val serverMotd: String = ServerInfo.motd,
    val userType: Byte = 0x00,
    val protocolVersion: Byte = 0x07,
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
