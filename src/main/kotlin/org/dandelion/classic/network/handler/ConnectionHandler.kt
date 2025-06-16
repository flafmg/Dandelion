package org.dandelion.classic.network.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import org.dandelion.classic.network.PacketFactory
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

    //sometimes the data dont arrive fully so we need to read it until it is complete, but first we need our packet implementation
    // * probably there is a better way to this but im dumb so im going to the dumb approach
    private fun processCompleteMessage(ctx: ChannelHandlerContext, channelKey: String) {
        var bufferToProcess = channelBuffers[channelKey] ?: return
        var offset = 0

        while (offset < bufferToProcess.size) {
            val packetId = bufferToProcess[offset]
            val expectedSize = PacketFactory.getPacketSize(packetId)

            if (expectedSize == -1) {
                System.err.println("Unknown packet ID: 0X%02X for channel $channelKey. Closing connection.".format(packetId))
                channelBuffers.remove(channelKey)
                ctx.close()
                return
            }
            if (expectedSize <= 0) {
                System.err.println("Invalid packet size ($expectedSize) for packet ID 0X%02X ($channelKey). Closing connection.".format(packetId))
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
                PacketFactory.handlePacket(ctx, packetData)
            } catch (e: Exception) {
                System.err.println("Error handling packet ID 0X%02X ($channelKey): ${e.message}. Closing connection.")
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