package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerSetTextHotKey(
    val label: String,
    val action: String,
    val keyCode: Int,
    val keyAddCtrl: Boolean,
    val keyAddShift: Boolean,
    val keyAddAlt: Boolean,
) : Packet() {
    override val id: Byte = 0x15
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeString(label)
        writer.writeString(action)
        writer.writeInt(keyCode)
        writer.writeByte(getKeyMods())
        return writer.toByteArray()
    }

    fun getKeyMods(): Byte {
        var mods = 0
        if (keyAddCtrl) mods = mods or 0x01
        if (keyAddShift) mods = mods or 0x02
        if (keyAddAlt) mods = mods or 0x04
        return mods.toByte()
    }
}
