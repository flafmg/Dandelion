package org.dandelion.classic.network.packets.cpe.server

import io.netty.channel.Channel
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.stream.PacketWriter

class ServerCinematicGui(
    val hideCrosshair: Boolean,
    val hideHotbar: Boolean,
    val hideHand: Boolean,
    val red: Byte,
    val green: Byte,
    val blue: Byte,
    val opacity: Byte,
    val apertureSize: Short,
) : Packet() {
    override val id: Byte = 0x38
    override val isCpe: Boolean = true

    override fun encode(channel: Channel): ByteArray {
        val writer = PacketWriter()
        writer.writeByte(id)
        writer.writeBoolean(hideCrosshair)
        writer.writeBoolean(hideHotbar)
        writer.writeBoolean(hideHand)
        writer.writeByte(red)
        writer.writeByte(green)
        writer.writeByte(blue)
        writer.writeByte(opacity)
        writer.writeShort(apertureSize)
        return writer.toByteArray()
    }
}
