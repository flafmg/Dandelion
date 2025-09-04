package org.dandelion.server.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.server.network.packets.Packet
import org.dandelion.server.network.packets.stream.PacketWriter

class ServerDefineEffect(
    val effectId: Byte,
    val u1: Byte,
    val v1: Byte,
    val u2: Byte,
    val v2: Byte,
    val redTint: Byte,
    val greenTint: Byte,
    val blueTint: Byte,
    val frameCount: Byte,
    val particleCount: Byte,
    val particleSize: Byte,
    val sizeVariation: Int,
    val spread: UShort,
    val speed: Int,
    val gravity: Int,
    val baseLifetime: Int,
    val lifetimeVariation: Int,
    val collideFlags: Byte,
    val fullBright: Byte,
) : Packet() {
    override val id: Byte = 0x30
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(effectId)
        writer.writeByte(u1)
        writer.writeByte(v1)
        writer.writeByte(u2)
        writer.writeByte(v2)
        writer.writeByte(redTint)
        writer.writeByte(greenTint)
        writer.writeByte(blueTint)
        writer.writeByte(frameCount)
        writer.writeByte(particleCount)
        writer.writeByte(particleSize)
        writer.writeInt(sizeVariation)
        writer.writeUShort(spread)
        writer.writeInt(speed)
        writer.writeInt(gravity)
        writer.writeInt(baseLifetime)
        writer.writeInt(lifetimeVariation)
        writer.writeByte(collideFlags)
        writer.writeByte(fullBright)
        return writer.toByteArray()
    }
}
