package org.dandelion.classic.network.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import org.dandelion.classic.player.PlayerManager

class DisconnectHandler : ChannelInboundHandlerAdapter() {
    override fun channelInactive(ctx: ChannelHandlerContext) {
        disconnectPlayer(ctx.channel())
        println("Client disconnected ${ctx.channel().remoteAddress()}")
        ctx.fireChannelInactive()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        println("Error exception for client ${ctx.channel().remoteAddress()}")
        disconnectPlayer(ctx.channel())
        ctx.close()
    }

    private fun disconnectPlayer(channel: Channel){
        PlayerManager.disconnectPlayer(channel)
    }
}
