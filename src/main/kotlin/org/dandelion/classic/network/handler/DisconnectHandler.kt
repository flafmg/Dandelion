package org.dandelion.classic.network.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.dandelion.classic.player.Players
import org.dandelion.classic.server.Console

class DisconnectHandler : ChannelInboundHandlerAdapter() {
    override fun channelInactive(ctx: ChannelHandlerContext) {
        disconnect(ctx.channel())
        Console.log("Client disconnected ${ctx.channel().remoteAddress()}")
        ctx.fireChannelInactive()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        Console.errLog("Error exception for client ${ctx.channel().remoteAddress()}")
        disconnect(ctx.channel())
        ctx.close()
    }

    private fun disconnect(channel: Channel){
        Players.handlePlayerDisconnection(channel)
    }
}