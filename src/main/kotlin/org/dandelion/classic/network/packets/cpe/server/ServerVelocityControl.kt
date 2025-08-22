package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerVelocityControl(
    val velocityX: Int,
    val velocityY: Int,
    val velocityZ: Int,
    val modeX: Byte,
    val modeY: Byte,
    val modeZ: Byte,
) : Packet() {
    override val id: Byte = 0x2F
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeInt(velocityX)
        writer.writeInt(velocityY)
        writer.writeInt(velocityZ)
        writer.writeByte(modeX)
        writer.writeByte(modeY)
        writer.writeByte(modeZ)
        return writer.toByteArray()
    }
}
