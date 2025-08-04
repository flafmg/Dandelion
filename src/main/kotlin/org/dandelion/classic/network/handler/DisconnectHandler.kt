package org.dandelion.classic.network.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.server.Console

class DisconnectHandler : ChannelInboundHandlerAdapter() {

    override fun channelInactive(ctx: ChannelHandlerContext) {
        val remoteAddress = ctx.channel().remoteAddress()

        try {
            handleDisconnection(ctx.channel(), "Channel inactive")
            Console.log("Client disconnected: $remoteAddress")
        } catch (e: Exception) {
            Console.errLog("Error handling disconnection for $remoteAddress: ${e.message}")
            forceDisconnection(ctx.channel())
        } finally {
            super.channelInactive(ctx)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        val remoteAddress = ctx.channel().remoteAddress()

        Console.errLog("Exception caught for client $remoteAddress: ${cause.message}")
        Console.debugLog("Exception stack trace: ${cause.stackTraceToString()}")

        try {
            handleDisconnection(ctx.channel(), "Exception: ${cause.message}")
        } catch (e: Exception) {
            Console.errLog("Error during exception handling for $remoteAddress: ${e.message}")
            forceDisconnection(ctx.channel())
        } finally {
            if (ctx.channel().isActive) {
                ctx.close()
            }
        }
    }

    private fun handleDisconnection(channel: Channel, reason: String) {
        try {
            val player = Players.find(channel)

            if (player != null) {
                Console.debugLog("Disconnecting player '${player.name}' due to: $reason")

                Players.handleDisconnection(channel)
            } else {
                val connectingPlayer = Players.getConnecting(channel)
                if (connectingPlayer != null) {
                    Console.debugLog("Disconnecting connecting player '${connectingPlayer.name}' due to: $reason")
                    Players.handleDisconnection(channel)
                } else {
                    Console.debugLog("Disconnecting unregistered client due to: $reason")
                }
            }
        } catch (e: Exception) {
            Console.errLog("Error in handleDisconnection: ${e.message}")
            throw e
        }
    }

    private fun forceDisconnection(channel: Channel) {
        try {
            Console.warnLog("Forcing disconnection cleanup for channel: ${channel.id().asShortText()}")
            Players.handleDisconnection(channel)
        } catch (e: Exception) {
            Console.errLog("Even force disconnection failed: ${e.message}")
        }
    }

    override fun channelRegistered(ctx: ChannelHandlerContext) {
        Console.debugLog("New client connected: ${ctx.channel().remoteAddress()}")
        super.channelRegistered(ctx)
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext) {
        val remoteAddress = ctx.channel().remoteAddress()

        try {
            handleDisconnection(ctx.channel(), "Channel unregistered")
            Console.debugLog("Client channel unregistered: $remoteAddress")
        } catch (e: Exception) {
            Console.errLog("Error during channel unregistered cleanup for $remoteAddress: ${e.message}")
            forceDisconnection(ctx.channel())
        } finally {
            super.channelUnregistered(ctx)
        }
    }
}