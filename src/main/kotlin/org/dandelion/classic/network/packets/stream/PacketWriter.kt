package org.dandelion.classic.network.packets.stream

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

internal class PacketWriter {
    private val output = ByteArrayOutputStream()

    fun writeByte(value: Byte) {
        output.write(value.toInt())
    }

    fun writeSByte(value: Byte) {
        output.write(value.toInt())
    }

    fun writeFByte(value: Float) {
        val fixed = (value * 32).toInt().coerceIn(-128, 127)
        output.write(fixed)
    }

    fun writeBoolean(value: Boolean) {
        writeByte(if (value) 1 else 0)
    }

    fun writeShort(value: Short) {
        output.write((value.toInt() shr 8) and 0xff)
        output.write(value.toInt() and 0xFF)
    }

    fun writeUShort(value: UShort) {
        output.write((value.toInt() shr 8) and 0xFF)
        output.write(value.toInt() and 0xFF)
    }

    fun writeFShort(value: Float) {
        val fixed = (value * 32.0f).toInt().coerceIn(-32768, 32767)
        output.write((fixed shr 8) and 0xFF)
        output.write(fixed and 0xff)
    }

    fun writeInt(value: Int) {
        output.write(ByteBuffer.allocate(4).putInt(value).array())
    }

    fun writeLong(value: Long) {
        output.write(ByteBuffer.allocate(8).putLong(value).array())
    }

    fun writeString(value: String, length: Int = 64) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        val padded = ByteArray(length) { 0x20 }
        System.arraycopy(bytes, 0, padded, 0, bytes.size.coerceAtMost(length))
        output.write(padded)
    }

    fun writeByteArray(data: ByteArray, length: Int = 1024) {
        val padded = ByteArray(length) { 0x00 }
        System.arraycopy(data, 0, padded, 0, data.size.coerceAtMost(length))
        output.write(padded)
    }

    fun writeFloat(value: Float) {
        val fixed = (value * 32.0f).toInt()
        output.write(ByteBuffer.allocate(4).putInt(fixed).array())
    }

    fun toByteArray(): ByteArray {
        return output.toByteArray()
    }
}
