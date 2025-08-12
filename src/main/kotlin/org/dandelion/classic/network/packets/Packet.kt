package org.dandelion.classic.network.packets

import io.netty.buffer.Unpooled
import io.netty.buffer.Unpooled.buffer
import io.netty.channel.Channel
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.server.Console

abstract class Packet {
    abstract val id: Byte
    open val size: Int = -1
    open val isCpe: Boolean = false
    var data: ByteArray = ByteArray(0)

    open fun decode(data: ByteArray) {}
    open fun encode(): ByteArray {return data}
    open fun resolve(channel: Channel){ }

    fun send(player: Player){
        send(player.channel)
    }
    fun send(channel: Channel) {
        val encodedData = encode()

        //buffer vier for debuging
        /*val buffer = encodedData
        println("buffer view")
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
        println("size is: ${buffer.size} bytes")*/


        if (!channel.isActive || !channel.isOpen) {
            Console.warnLog("Channel is not open, disconnecting client")
            channel.disconnect()
            return
        }

        try{
            channel.writeAndFlush(Unpooled.wrappedBuffer(encodedData)).addListener { future ->
                if(!future.isSuccess){
                    Console.errLog("Failed to send packet, disconecting client")
                    channel.disconnect()
                }
            }
        }catch (ex: Exception){
            Console.errLog("Failed to send packet ${ex.message}")
            channel.disconnect()
        }
    }
    fun send(list: List<*>) { //stupif plataform declaration clash
        when {
            list.isNotEmpty() && list[0] is Player -> list.forEach { send(it as Player) }
            list.isNotEmpty() && list[0] is Channel -> list.forEach { send(it as Channel) }
        }
    }

}