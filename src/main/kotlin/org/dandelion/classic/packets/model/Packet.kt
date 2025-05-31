package org.dandelion.classic.packets.model

import io.netty.channel.Channel
import org.dandelion.classic.Console

abstract class Packet {
    abstract val id: Byte
    open val size: Int = 0
    var data: ByteArray = ByteArray(0)

    open fun decode(data: ByteArray) { }
    open fun encode(): ByteArray { return data }
    open fun resolve(channel: Channel) { }

    fun sendNetty(channel: Channel) {
        val encoded = encode()
        if (!channel.isActive || !channel.isOpen) {
            Console.errLog("Channel is not active or open for packet $id")
            return
        }
        try {
            channel.writeAndFlush(io.netty.buffer.Unpooled.wrappedBuffer(encoded)).addListener { future ->
                if (future.isSuccess) {
                    val hex = encoded.joinToString(" ") { "%02X".format(it) }
                    Console.debugLog("Sent packet $id (${encoded.size} bytes): $hex")
                } else {
                    Console.errLog("Failed to send packet $id: ${future.cause()?.message}")
                }
            }
        } catch (e: Exception) {
            Console.errLog("Exception sending packet $id: ${e.message}")
        }
    }
}
