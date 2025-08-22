package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerHackControl(
    val flying: Boolean,
    val noCLip: Boolean,
    val speeding: Boolean,
    val spawnControl: Boolean,
    val thirdPerson: Boolean,
    val jumpHeight: Short,
) : Packet() {
    override val id: Byte = 0x20
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeBoolean(flying)
        writer.writeBoolean(noCLip)
        writer.writeBoolean(speeding)
        writer.writeBoolean(spawnControl)
        writer.writeBoolean(thirdPerson)
        writer.writeShort(jumpHeight)
        return writer.toByteArray()
    }
}
