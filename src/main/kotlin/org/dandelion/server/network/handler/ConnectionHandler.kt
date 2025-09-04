package org.dandelion.server.network.handler

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import java.util.concurrent.ConcurrentHashMap
import org.dandelion.server.entity.player.Players
import org.dandelion.server.network.PacketRegistry
import org.dandelion.server.server.Console

class ConnectionHandler : SimpleChannelInboundHandler<ByteBuf>() {
    companion object {
        private val channelBuffers = ConcurrentHashMap<String, ByteArray>()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val channelKey = ctx.channel().id().asShortText()

        try {
            val newBytes = ByteArray(msg.readableBytes())
            msg.readBytes(newBytes)
            val buffer =
                channelBuffers.getOrDefault(channelKey, ByteArray(0)) + newBytes
            channelBuffers[channelKey] = buffer
            processCompleteMessage(ctx, channelKey)
        } catch (e: Exception) {
            Console.errLog(
                "Error reading data from channel $channelKey: ${e.message}"
            )
            handleChannelError(ctx, channelKey, "Data reading error")
        }
    }

    private fun processCompleteMessage(
        ctx: ChannelHandlerContext,
        channelKey: String,
    ) {
        var bufferToProcess = channelBuffers[channelKey] ?: return
        var offset = 0

        try {
            while (offset < bufferToProcess.size) {
                val packetId = bufferToProcess[offset]
                val expectedSize =
                    PacketRegistry.getPacketSize(packetId, ctx.channel())

                when {
                    expectedSize == -1 -> {
                        Console.errLog(
                            "Unknown packet ID: 0X%02X for channel $channelKey"
                                .format(packetId)
                        )
                        handleChannelError(
                            ctx,
                            channelKey,
                            "Unknown packet ID: 0X%02X".format(packetId),
                        )
                        return
                    }
                    expectedSize <= 0 -> {
                        Console.errLog(
                            "Invalid packet size ($expectedSize) for packet ID 0X%02X ($channelKey)"
                                .format(packetId)
                        )
                        handleChannelError(
                            ctx,
                            channelKey,
                            "Invalid packet size",
                        )
                        return
                    }
                    offset + expectedSize > bufferToProcess.size -> {
                        val remainingData =
                            bufferToProcess.copyOfRange(
                                offset,
                                bufferToProcess.size,
                            )
                        channelBuffers[channelKey] = remainingData
                        return
                    }
                }

                val packetData =
                    bufferToProcess.copyOfRange(offset, offset + expectedSize)

                try {
                    PacketRegistry.handlePacket(ctx, packetData)
                } catch (e: Exception) {
                    Console.errLog(
                        "Error handling packet ID 0X%02X ($channelKey): ${e.message}"
                            .format(packetId)
                    )
                    Console.errLog("Stack trace: ${e.stackTraceToString()}")
                    handleChannelError(ctx, channelKey, "Packet handling error")
                    return
                }

                offset += expectedSize
            }

            val finalRemainingData =
                bufferToProcess.copyOfRange(offset, bufferToProcess.size)
            channelBuffers[channelKey] = finalRemainingData
        } catch (e: Exception) {
            Console.errLog(
                "Unexpected error processing messages for channel $channelKey: ${e.message}"
            )
            Console.errLog("Stack trace: ${e.stackTraceToString()}")
            handleChannelError(ctx, channelKey, "Message processing error")
        }
    }

    private fun handleChannelError(
        ctx: ChannelHandlerContext,
        channelKey: String,
        reason: String,
    ) {
        try {
            channelBuffers.remove(channelKey)
            Players.forceDisconnect(ctx.channel())

            if (ctx.channel().isActive) {
                ctx.close()
            }
        } catch (e: Exception) {
            Console.errLog(
                "Error during channel cleanup for $channelKey: ${e.message}"
            )
            ctx.close()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        val channelKey = ctx.channel().id().asShortText()
        Console.errLog(
            "Exception caught in ConnectionHandler for channel $channelKey: ${cause.message}"
        )
        Console.errLog("Stack trace: ${cause.stackTraceToString()}")

        handleChannelError(
            ctx,
            channelKey,
            "Exception in handler: ${cause.message}",
        )
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        val channelKey = ctx.channel().id().asShortText()

        try {
            channelBuffers.remove(channelKey)
            Players.handleDisconnection(ctx.channel())

            Console.debugLog(
                "Channel $channelKey became inactive, cleaned up resources"
            )
        } catch (e: Exception) {
            Console.errLog(
                "Error during channel inactive cleanup for $channelKey: ${e.message}"
            )
        } finally {
            super.channelInactive(ctx)
        }
    }
}
