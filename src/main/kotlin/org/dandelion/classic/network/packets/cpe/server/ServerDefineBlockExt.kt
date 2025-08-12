package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerDefineBlockExt(
    val blockId: Byte,
    val name: String,
    val solidity: Byte,
    val movementSpeed: Byte,
    val topTextureId: Byte,
    val leftTextureId: Byte,
    val rightTextureId: Byte,
    val frontTextureId: Byte,
    val backTextureId: Byte,
    val bottomTextureId: Byte,
    val transmitsLight: Boolean,
    val walkSound: Byte,
    val fullBright: Boolean,
    val minX: Byte,
    val minY: Byte,
    val minZ: Byte,
    val maxX: Byte,
    val maxY: Byte,
    val maxZ: Byte,
    val blockDraw: Byte,
    val fogDensity: Byte,
    val fogR: Byte,
    val fogG: Byte,
    val fogB: Byte
) : Packet() {
    override val id: Byte = 0x25
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)

        writer.writeByte(blockId)
        writer.writeString(name)

        writer.writeByte(solidity)

        writer.writeByte(movementSpeed)
        writer.writeByte(topTextureId)
        writer.writeByte(leftTextureId)
        writer.writeByte(rightTextureId)
        writer.writeByte(frontTextureId)
        writer.writeByte(backTextureId)
        writer.writeByte(bottomTextureId)

        writer.writeBoolean(transmitsLight)
        writer.writeByte(walkSound)
        writer.writeBoolean(fullBright)

        writer.writeByte(minX)
        writer.writeByte(minY)
        writer.writeByte(minZ)
        writer.writeByte(maxX)
        writer.writeByte(maxY)
        writer.writeByte(maxZ)

        writer.writeByte(blockDraw)
        writer.writeByte(fogDensity)

        writer.writeByte(fogR)
        writer.writeByte(fogG)
        writer.writeByte(fogB)
        return writer.toByteArray()
    }
}
