package org.dandelion.classic.server.packets.manager

import kotlinx.coroutines.*
import org.dandelion.classic.server.data.player.manager.PlayerManager
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.client.ClientIndentification
import org.dandelion.classic.server.packets.client.ClientMessage
import org.dandelion.classic.server.packets.client.PositionAndOrientation
import org.dandelion.classic.server.packets.client.SetBlock
import org.dandelion.classic.server.util.Logger
import java.util.concurrent.ConcurrentHashMap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelHandler.Sharable

object PacketManager {
    private val packetFactory = ConcurrentHashMap<Byte, () -> Packet>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun registerPacket(id: Byte, factory: () -> Packet) {
        packetFactory[id] = factory
    }

    fun createPacket(id: Byte): Packet? = packetFactory[id]?.invoke()

    fun getPacketSize(id: Byte): Int {
        val packet = createPacket(id)
        return packet?.size ?: -1
    }

    fun init() {
        registerPacket(0x00, ::ClientIndentification)
        registerPacket(0x05, ::SetBlock)
        registerPacket(0x08, ::PositionAndOrientation)
        registerPacket(0x0d, ::ClientMessage)
        Logger.log("PacketManager initialized")
    }

    fun handlePacket(ctx: ChannelHandlerContext, bytes: ByteArray) {
        if (bytes.isEmpty()) {
            Logger.warnLog("Received empty packet from ${ctx.channel().remoteAddress()}")
            return
        }

        val packetId = bytes[0]
        val packet = createPacket(packetId)

        if (packet == null) {
            Logger.warnLog("Unknown packet id: 0x%02X from ${ctx.channel().remoteAddress()}".format(packetId))
            return
        }

        try {
            if (bytes.size != packet.size) {
                Logger.errLog("Packet size mismatch for id 0x%02X: expected %d, got %d from %s".format(
                    packetId, packet.size, bytes.size, ctx.channel().remoteAddress()
                ))
                return
            }

            packet.decode(bytes)
            packet.resolve(ctx.channel())

        } catch (e: IllegalArgumentException) {
            Logger.errLog("Invalid packet data for id 0x%02X from %s: %s".format(
                packetId, ctx.channel().remoteAddress(), e.message
            ))
        } catch (e: Exception) {
            Logger.errLog("Error processing packet id 0x%02X from %s: %s".format(
                packetId, ctx.channel().remoteAddress(), e.message
            ))
            e.printStackTrace()
        }
    }

    @Sharable
    class NettyDisconnectHandler : ChannelInboundHandlerAdapter() {
        override fun channelInactive(ctx: ChannelHandlerContext) {
            Logger.debugLog("Netty channelInactive: ${ctx.channel().remoteAddress()}")
            GlobalScope.launch {
                try {
                    val player = PlayerManager.getPlayerByChannel(ctx.channel())
                    if (player != null) {
                        Logger.debugLog("Desconectando player ${player.userName} (id=${player.playerID}) do canal ${ctx.channel().remoteAddress()}")
                        PlayerManager.playerDisconnect(player.levelId, player.playerID)
                    } else {
                        Logger.debugLog("Nenhum player encontrado para o canal ${ctx.channel().remoteAddress()}")
                    }
                } catch (e: Exception) {
                    Logger.errLog("Error during player disconnect for ${ctx.channel().remoteAddress()}: ${e.message}")
                }
            }
            ctx.fireChannelInactive()
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            Logger.errLog("Channel exception for ${ctx.channel().remoteAddress()}: ${cause.message}")
            val player = PlayerManager.getPlayerByChannel(ctx.channel())
            if (player != null) {
                PlayerManager.playerDisconnect(player.levelId, player.playerID)
            }
            ctx.close()
        }
    }
}

