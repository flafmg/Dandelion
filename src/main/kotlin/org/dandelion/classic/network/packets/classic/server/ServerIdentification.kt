package org.dandelion.classic.network.packets.classic.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter
import org.dandelion.classic.server.Server

class ServerIdentification: Packet() {
    override val id: Byte = 0x00

    private val protocolVersion: Byte = 0x07 // server only accepts protocol 0x07
    private val serverName: String = Server.name
    private val serverMotd: String = Server.motd
    private val userType: Byte = 0x00

    override fun encode(): ByteArray {
        val writer = PacketWriter()

        writer.writeByte(id) // need to put id first aways
        writer.writeByte(protocolVersion)
        writer.writeString(serverName)
        writer.writeString(serverMotd)
        writer.writeByte(userType)

        return writer.toByteArray()
    }

    override fun resolve(channel: Channel) {
        send(channel)
    }
}