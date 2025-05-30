package org.dandelion.classic.server.manager

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import org.dandelion.classic.server.config.manager.ServerConfigManager
import org.dandelion.classic.server.data.player.manager.PlayerManager
import org.dandelion.classic.server.packets.manager.PacketManager
import org.dandelion.classic.server.util.Logger
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager() {
    private val isRunning = AtomicBoolean(false)
    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private var channel: Channel? = null

    fun start() {
        if (isRunning.getAndSet(true)) return
        val port = ServerConfigManager.serverConfig.get().serverSettings.port
        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        Logger.debugLog("client connected ${ch.remoteAddress()}")
                        ch.pipeline().addLast(PacketManager.NettyDisconnectHandler())
                        ch.pipeline().addLast(PacketHandler(PacketManager))
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
            Logger.log("ConnectionManager starting on port: $port")
            val f = b.bind(InetSocketAddress(port)).sync()
            channel = f.channel()
            Logger.log("ConnectionManager active on port: $port")
        } catch (e: Exception) {
            Logger.errLog("ConnectionManager error: ${e.message}")
        }
    }

    fun stop() {
        Logger.log("Stopping ConnectionManager...")
        isRunning.set(false)
        try { channel?.close()?.sync() } catch (_: Exception) {}
        try { bossGroup?.shutdownGracefully() } catch (_: Exception) {}
        try { workerGroup?.shutdownGracefully() } catch (_: Exception) {}
        Logger.log("ConnectionManager stopped!")
    }

    private class PacketHandler(private val packetManager: PacketManager) : SimpleChannelInboundHandler<ByteBuf>() {
        companion object {
            private val channelBuffers = ConcurrentHashMap<String, ByteArray>()
        }

        override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
            val channelKey = ctx.channel().id().asShortText()
            val newBytes = ByteArray(msg.readableBytes())
            msg.readBytes(newBytes)
            val buffer = channelBuffers.getOrDefault(channelKey, ByteArray(0)) + newBytes
            channelBuffers[channelKey] = buffer
            processCompletePackets(ctx, channelKey)
        }

        private fun processCompletePackets(ctx: ChannelHandlerContext, channelKey: String) {
            val buffer = channelBuffers[channelKey] ?: return
            var offset = 0
            while (offset < buffer.size) {
                if (offset >= buffer.size) break
                val packetId = buffer[offset]
                val expectedSize = packetManager.getPacketSize(packetId)
                if (expectedSize == -1) {
                    Logger.warnLog("Unknown packet id: 0x%02X, skipping byte".format(packetId))
                    offset++
                    continue
                }
                if (offset + expectedSize > buffer.size) {
                    break
                }
                val packetData = buffer.sliceArray(offset until offset + expectedSize)
                packetManager.handlePacket(ctx, packetData)
                offset += expectedSize
            }
            if (offset > 0) {
                val remainingBuffer = if (offset < buffer.size) {
                    buffer.sliceArray(offset until buffer.size)
                } else {
                    ByteArray(0)
                }
                channelBuffers[channelKey] = remainingBuffer
            }
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            val channelKey = ctx.channel().id().asShortText()
            channelBuffers.remove(channelKey)
            super.channelInactive(ctx)
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            Logger.errLog("Netty client error: ${cause::class.simpleName}: ${cause.message}")
            cause.printStackTrace()
            val channelKey = ctx.channel().id().asShortText()
            channelBuffers.remove(channelKey)
            val player = PlayerManager.getPlayerByChannel(ctx.channel())
            if (player != null) {
                PlayerManager.playerDisconnect(player.levelId, player.playerID)
            }
            ctx.close()
        }
    }
}
