package org.dandelion.classic.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter
import org.dandelion.classic.server.Server

class ServerIdentification(
    val serverName: String = Server.name,
    val serverMotd: String = Server.motd,
    val userType: Byte = 0x00,
    val protocolVersion: Byte = 0x07,

): Packet() {
    override val id: Byte = 0x00
    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id)
        writer.writeByte(protocolVersion)
        writer.writeString(serverName)
        writer.writeString(serverMotd)
        writer.writeByte(userType)
        return writer.toByteArray()
    }
}