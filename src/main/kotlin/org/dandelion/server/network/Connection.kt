package org.dandelion.server.network

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.dandelion.server.network.handler.ConnectionHandler
import org.dandelion.server.network.handler.DisconnectHandler
import org.dandelion.server.server.Console
import org.dandelion.server.server.Server
import org.dandelion.server.server.data.ServerConfig

internal object Connection {
    private var isRunning = false

    private var bossGroup: EventLoopGroup? = null
    private var workerGroup: EventLoopGroup? = null
    private var channel: Channel? = null

    fun init() {
        if (isRunning) return
        isRunning = true

        Console.log(
            "Initializing connection manager on port ${ServerConfig.port}"
        )

        bossGroup =
            NioEventLoopGroup() // this group accept and estabilishes the client connection
        workerGroup = NioEventLoopGroup() // handle estabilhed connectoins

        try {
            var serverBootstrap = ServerBootstrap()
            serverBootstrap
                .group(bossGroup, workerGroup) // sets group in serverbootstrap
                .channel(
                    NioServerSocketChannel::class.java
                ) // add tcp connection
                .childHandler(
                    object : ChannelInitializer<SocketChannel>() {
                        override fun initChannel(
                            ch: SocketChannel
                        ) { // handle here bc its easier
                            Console.log(
                                "Client connected ${ch.remoteAddress()}"
                            )
                            ch.pipeline()
                                .addLast(
                                    DisconnectHandler()
                                ) // here we add the disconect handler (so we dont have to use the
                            // ping packet lol)
                            ch.pipeline()
                                .addLast(
                                    ConnectionHandler()
                                ) // here we will add the pccket connection handler
                        }
                    }
                )
                .childOption(
                    ChannelOption.SO_KEEPALIVE,
                    true,
                ) // keeps the connection alive (duh)

            val channelFuture =
                serverBootstrap
                    .bind(ServerConfig.port)
                    .sync() // starts connection on the port
            channel = channelFuture.channel()
            Console.log(
                "connection manager active on port ${ServerConfig.port}"
            )
        } catch (ex: Exception) {
            Console.errLog(
                "Exception occured while trying to enable connection manager: ${ex.message}"
            )
            Server.shutdown()
        }
    }

    fun shutdown() {
        if (!isRunning) return
        isRunning = false

        Console.log("Stopping connection manager...")
        // we trow errors out, it is shutting down anyways
        try {
            channel?.close()?.sync()
        } catch (_: Exception) {}
        try {
            bossGroup?.shutdownGracefully()
        } catch (_: Exception) {}
        try {
            workerGroup?.shutdownGracefully()
        } catch (_: Exception) {}
        Console.log("connectionManager stoped")
    }
}
