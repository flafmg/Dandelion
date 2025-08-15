package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerExtAddEntity2(
    val entityId: Byte,
    val inGameName: String,
    val skinName: String,
    val spawnX: Short,
    val spawnY: Short,
    val spawnZ: Short,
    val spawnYaw: Byte,
    val spawnPitch: Byte,
) : Packet() {
    override val id: Byte = 0x21
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(entityId)
        writer.writeString(inGameName)
        writer.writeString(skinName)
        writer.writeShort(spawnX)
        writer.writeShort(spawnY)
        writer.writeShort(spawnZ)
        writer.writeByte(spawnYaw)
        writer.writeByte(spawnPitch)
        return writer.toByteArray()
    }
}
