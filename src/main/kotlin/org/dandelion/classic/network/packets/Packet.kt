package org.dandelion.classic.network.packets

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import org.dandelion.classic.entity.Player
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

}