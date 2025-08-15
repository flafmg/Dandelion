package org.dandelion.classic.network.packets.cpe.server

import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

/**
 * ChangeModel packet for changing the model of an entity.
 *
 * This packet allows changing the visual model of any entity in the game.
 * The model name can be one of the predefined models or a valid block ID as a string.
 *
 * @property entityId The ID of the entity to change the model for (0-127)
 * @property modelName The name of the model to use or a valid block ID as string
 */
class ServerChangeModel(
    val entityId: Byte,
    val modelName: String,
) : Packet() {
    override val id: Byte = 0x1D
    override val isCpe: Boolean = true

    override fun encode(): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeByte(entityId)
        writer.writeString(modelName)
        return writer.toByteArray()
    }
}
