package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerDefineBlock(
    val blockId: Byte,
    val name: String,
    val solidity: Byte,
    val movementSpeed: Byte,
    val topTextureId: Byte,
    val sideTextureId: Byte,
    val bottomTextureId: Byte,
    val transmitsLight: Boolean,
    val walkSound: Byte,
    val fullBright: Boolean,
    val shape: Byte,
    val blockDraw: Byte,
    val fogDensity: Byte,
    val fogR: Byte,
    val fogG: Byte,
    val fogB: Byte
) : Packet() {
    override val id: Byte = 0x23
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(blockId)
        writer.writeString(name)

        writer.writeByte(solidity)
        writer.writeByte(movementSpeed)

        writer.writeByte(topTextureId)
        writer.writeByte(sideTextureId)
        writer.writeByte(bottomTextureId)

        writer.writeBoolean(transmitsLight)
        writer.writeByte(walkSound)

        writer.writeBoolean(fullBright)

        writer.writeByte(shape)

        writer.writeByte(blockDraw)
        writer.writeByte(fogDensity)
        writer.writeByte(fogR)
        writer.writeByte(fogG)
        writer.writeByte(fogB)
        return writer.toByteArray()
    }
}
