package org.dandelion.classic.network.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import org.dandelion.classic.network.PacketRegistry
import org.dandelion.classic.server.Console
import java.util.concurrent.ConcurrentHashMap

class ConnectionHandler : SimpleChannelInboundHandler<ByteBuf>() {
    companion object{
        private val channelBuffers = ConcurrentHashMap<String, ByteArray>() // our buffers for the clients
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val channelKey = ctx.channel().id().asShortText()
        val newBytes = ByteArray(msg.readableBytes());
        msg.readBytes(newBytes)
        val buffer = channelBuffers.getOrDefault(channelKey, ByteArray(0)) + newBytes
        channelBuffers[channelKey] = buffer
        processCompleteMessage(ctx, channelKey)
    }

    private fun processCompleteMessage(ctx: ChannelHandlerContext, channelKey: String) {
        var bufferToProcess = channelBuffers[channelKey] ?: return
        var offset = 0

        while (offset < bufferToProcess.size) {
            val packetId = bufferToProcess[offset]
            val expectedSize = PacketRegistry.getPacketSize(packetId)

            if (expectedSize == -1) {
                Console.errLog("Unknown packet ID: 0X%02X for channel $channelKey. Closing connection.".format(packetId))
                channelBuffers.remove(channelKey)
                ctx.close()
                return
            }
            if (expectedSize <= 0) {
                Console.errLog("Invalid packet size ($expectedSize) for packet ID 0X%02X ($channelKey). Closing connection.".format(packetId))
                channelBuffers.remove(channelKey)
                ctx.close()
                return
            }
            if (offset + expectedSize > bufferToProcess.size) {
                val remainingData = bufferToProcess.copyOfRange(offset, bufferToProcess.size)
                channelBuffers[channelKey] = remainingData
                return
            }

            val packetData = bufferToProcess.copyOfRange(offset, offset + expectedSize)
            try {
                PacketRegistry.handlePacket(ctx, packetData)
            } catch (e: Exception) {
                Console.errLog("Error handling packet ID 0X%02X ($channelKey): ${e.message}. Closing connection.")
                channelBuffers.remove(channelKey)
                ctx.close()
                return
            }

            offset += expectedSize
        }

        val finalRemainingData = bufferToProcess.copyOfRange(offset, bufferToProcess.size)
        channelBuffers[channelKey] = finalRemainingData
    }

}