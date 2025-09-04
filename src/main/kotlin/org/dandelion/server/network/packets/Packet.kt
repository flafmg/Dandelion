package org.dandelion.server.network.packets

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import org.dandelion.server.entity.player.Player

abstract class Packet {
    abstract val id: Byte
    open val size: Int = -1
    open val sizeOverrides: MutableMap<String, Int> = mutableMapOf()
    open val isCpe: Boolean = false
    var data: ByteArray = ByteArray(0)

    open fun decode(data: ByteArray, channel: Channel) {}

    open fun encode(channel: Channel): ByteArray {
        return data
    }

    open fun resolve(channel: Channel) {}

    fun send(player: Player) {
        send(player.channel)
    }

    fun send(channel: Channel) {
        val encodedData = encode(channel)

        // Buffer hex dump for debugging
        /*val buffer = encodedData
        val packetName = this::class.simpleName ?: "UnknownPacket"
        println("=== Buffer view for $packetName (ID: 0x${String.format("%02X", id.toInt() and 0xFF)}(${id.toInt() and 0xFF})) ===")
        buffer.forEachIndexed { index, byte ->
            if (index % 16 == 0) {
                if (index != 0) {
                    print(" | ")
                    for (i in (index - 16) until index) {
                        val b = buffer.getOrNull(i) ?: 0
                        print(if (b in 32..126) b.toInt().toChar() else '.')
                    }
                    println()
                }
                print(String.format("%04X: ", index))
            }
            print(String.format("%02X ", byte.toInt() and 0xFF))
            if (index == buffer.size - 1) {
                val pad = 16 - (buffer.size % 16)
                if (pad != 16) repeat(pad) { print("   ") }
                print(" | ")
                for (i in (index - (index % 16))..index) {
                    val b = buffer.getOrNull(i) ?: 0
                    print(if (b in 32..126) b.toInt().toChar() else '.')
                }
                println()
            }
        }
        println("=== End of $packetName - Size: ${buffer.size} bytes ===")*/

        if (!channel.isActive || !channel.isOpen) {
            return
        }

        try {
            channel
                .writeAndFlush(Unpooled.wrappedBuffer(encodedData))
                .addListener { future ->
                    if (!future.isSuccess) {
                        channel.close()
                    }
                }
        } catch (ex: Exception) {
            channel.close()
        }
    }

    fun send(list: List<*>) { // stupif plataform declaration clash
        when {
            list.isNotEmpty() && list[0] is Player ->
                list.forEach { send(it as Player) }
            list.isNotEmpty() && list[0] is Channel ->
                list.forEach { send(it as Channel) }
        }
    }
}
