package org.dandelion.classic.network

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import java.util.concurrent.ConcurrentHashMap
import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.classic.client.ClientIdentification
import org.dandelion.classic.network.packets.classic.client.ClientMessage
import org.dandelion.classic.network.packets.classic.client.ClientPositionAndOrientation
import org.dandelion.classic.network.packets.classic.client.ClientSetBlock
import org.dandelion.classic.network.packets.cpe.client.ClientCustomBlockLevel
import org.dandelion.classic.network.packets.cpe.client.ClientExtEntry
import org.dandelion.classic.network.packets.cpe.client.ClientExtInfo
import org.dandelion.classic.network.packets.cpe.client.ClientNotifyAction
import org.dandelion.classic.network.packets.cpe.client.ClientNotifyPositionAction
import org.dandelion.classic.network.packets.cpe.client.ClientPlayerClick
import org.dandelion.classic.network.packets.cpe.server.ServerCustomBlockLevel
import org.dandelion.classic.network.packets.cpe.server.ServerExtEntry
import org.dandelion.classic.network.packets.cpe.server.ServerExtInfo
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.Server

object PacketRegistry {
    private val packetFactory = ConcurrentHashMap<Byte, () -> Packet>()
    private val supportedCPE = mutableListOf<Pair<String, Int>>()

    internal fun init() {
        Console.log("initializing packet handler...")
        registerPackets()
        registerSupportedCPE()
    }

    private fun registerPackets() {
        // classic
        registerPacket(0x00, ::ClientIdentification)
        registerPacket(0x05, ::ClientSetBlock)
        registerPacket(0x08, ::ClientPositionAndOrientation)
        registerPacket(0x0D, ::ClientMessage)

        // cpe
        registerPacket(0x10, ::ClientExtInfo)
        registerPacket(0x11, ::ClientExtEntry)
        registerPacket(0x13, ::ClientCustomBlockLevel)
        registerPacket(0x39, ::ClientNotifyAction)
        registerPacket(0x3A, ::ClientNotifyPositionAction)
        registerPacket(0x22, ::ClientPlayerClick)
    }

    private fun registerSupportedCPE() {
        addCPE("EnvColors")
        addCPE("EnvWeatherType")
        addCPE("EnvMapAspect")
        addCPE("CustomBlocks")
        addCPE("ClickDistance")
        addCPE("BlockPermissions")
        addCPE("HackControl")
        addCPE("HeldBlock")
        addCPE("SetHotbar")
        addCPE("SetSpawnPoint")
        addCPE("MessageTypes")
        addCPE("InstantMOTD")
        addCPE("LongerMessages")
        addCPE("BlockDefinitions")
        addCPE("BlockDefinitionsExt", 2)
        addCPE("ExtPlayerList", 2)
        addCPE("ChangeModel")
        addCPE("VelocityControl")
        addCPE("InventoryOrder")
        addCPE("SelectionCuboid")
        addCPE("EntityProperty")
        addCPE("BulkBlockUpdate")
        addCPE("NotifyAction")
        addCPE("LightingMode")
        addCPE("CinematicGUI")
        addCPE("ToggleBlockList")
        addCPE("FastMap")
        addCPE("ExtendedTextures")
        addCPE("ExtendedBlocks")
        addCPE("PlayerClick")
        addCPE("FullCP437")
        addCPE("ExtEntityPositions")
        addCPE("ExtEntityTeleport")
        addCPE("CustomParticles")
    }

    fun registerPacket(id: Byte, factory: () -> Packet) {
        if (packetFactory.contains(id)) {
            Console.warnLog("Packet of id $id is already registered")
            return
        }
        packetFactory[id] = factory
    }

    fun unregisterPacket(id: Byte) {
        if (packetFactory.contains(id)) {
            packetFactory.remove(id)
        }
    }

    private fun unregisterAllPackets() {
        packetFactory.keys.forEach { key -> unregisterPacket(key) }
    }

    fun createPacket(id: Byte): Packet? {
        if (packetFactory.contains(id)) {
            Console.warnLog("Packet of id $id is not registered")
            return null
        }
        return packetFactory[id]?.invoke()
    }

    fun getPacketSize(id: Byte, channel: Channel): Int {
        val packet = createPacket(id)
        var packetSize = -1
        if (packet != null) {
            val player = Players.find(channel)
            packetSize = packet.size
            packet.sizeOverrides.forEach { (ext, value) ->
                if (player?.supports(ext) == true) {
                    packetSize += value
                }
            }
        }
        return packetSize
    }

    internal fun handlePacket(ctx: ChannelHandlerContext, data: ByteArray) {
        if (data.isEmpty()) {
            Console.errLog(
                "received data is empty from ${ctx.channel().remoteAddress()}"
            )
            return
        }

        val packetId = data[0]
        val packet = createPacket(packetId)

        if (packet == null) {
            Console.warnLog("Packet $packetId doesnt exist, closing connection")
            ctx.close()
            return
        }

        try {
            packet.decode(data, ctx.channel())
            packet.resolve(ctx.channel())
        } catch (ex: Exception) {
            Console.errLog(
                "Error processing packet $packetId: ${ex.message} ${ex.stackTraceToString()}"
            )
            ctx.close()
        }
    }

    internal fun sendCPEHandshake(player: Player) {
        val count = supportedCPE.size.toShort()
        ServerExtInfo(Server.getSoftware(), count).send(player)
    }

    internal fun handleCPEHandshake(info: ClientExtInfo, channel: Channel) {
        val count = supportedCPE.size.toShort()
        val player = Players.getConnecting(channel)
        if (player == null) {
            return
        }

        player.supportedCpeCount = info.extensionCount
        player.client = info.appName
    }

    // Handles received CPE from player
    internal fun handleCPEEntry(extEntry: ClientExtEntry, channel: Channel) {
        val player = Players.getConnecting(channel)
        if (player == null) {
            return
        }

        val extName = extEntry.extName
        val version = extEntry.version

        player.addCPE(extName, version)

        if (player.getCPE().size.toShort() == player.supportedCpeCount) {
            handleClientPostCPE(player)
            Players.finalizeHandshake(player)
        }
    }

    // Sends supportedCPE
    internal fun sendCPEEntries(player: Player) {
        supportedCPE.forEach { (extName, version) ->
            ServerExtEntry(extName, version).send(player)
        }
    }

    // handle client postCPe and before ServerInfo
    internal fun handleClientPostCPE(player: Player) {
        if (player.supports("CustomBlocks")) {
            ServerCustomBlockLevel(1).send(player)
        }
    }

    fun addCPE(name: String, version: Int) {
        if (!supportedCPE.any { it.first == name && it.second == version }) {
            supportedCPE.add(name to version)
        }
    }

    fun addCPE(name: String) {
        addCPE(name, 1)
    }

    fun supports(name: String, version: Int? = null): Boolean {
        return if (version == null) {
            supportedCPE.any { it.first == name }
        } else {
            supportedCPE.any { it.first == name && it.second == version }
        }
    }

    fun getSupportedCPE(): List<Pair<String, Int>> = supportedCPE.toList()
}
