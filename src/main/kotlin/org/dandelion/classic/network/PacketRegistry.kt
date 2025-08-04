package org.dandelion.classic.network

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
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
import org.dandelion.classic.network.packets.cpe.server.ServerCustomBlockLevel
import org.dandelion.classic.network.packets.cpe.server.ServerExtEntry
import org.dandelion.classic.network.packets.cpe.server.ServerExtInfo
import org.dandelion.classic.server.Console
import org.dandelion.classic.server.Server
import java.util.concurrent.ConcurrentHashMap

object PacketRegistry {
    private val packetFactory = ConcurrentHashMap<Byte, () -> Packet>()
    private val supportedCPE = mutableListOf<Pair<String, Int>>()

    internal fun init() {
        Console.log("initializing packet handler...")
        registerPackets()
        registerSupportedCPE()
    }

    private fun registerPackets(){
        //classic
        registerPacket(0x00, ::ClientIdentification)
        registerPacket(0x05, ::ClientSetBlock)
        registerPacket(0x08, ::ClientPositionAndOrientation)
        registerPacket(0x0D, ::ClientMessage)

        //cpe
        registerPacket(0x10, ::ClientExtInfo)
        registerPacket(0x11, ::ClientExtEntry)
        registerPacket(0x13, ::ClientCustomBlockLevel)
    }
    private fun registerSupportedCPE(){
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
    }

    fun registerPacket(id: Byte, factory : () -> Packet){
        if(packetFactory.contains(id)){
            Console.warnLog("Packet of id $id is already registered")
            return
        }
        packetFactory[id] = factory
    }
    fun unregisterPacket(id: Byte){
        if(packetFactory.contains(id)) {
            packetFactory.remove(id)
        }
    }
    private fun unregisterAllPackets(){
        packetFactory.keys.forEach { key -> unregisterPacket(key)}
    }
    fun createPacket(id: Byte): Packet? {
        if(packetFactory.contains(id)){
            Console.warnLog("Packet of id $id is not registered")
            return null
        }
        return packetFactory[id]?.invoke()
    }

    fun getPacketSize(id: Byte): Int{
        val packet = createPacket(id);
        return packet?.size ?: -1
    }

    internal fun handlePacket(ctx: ChannelHandlerContext, data: ByteArray){
        if(data.isEmpty()){
            Console.errLog("received data is empty from ${ctx.channel().remoteAddress()}")
            return
        }

        val packetId = data[0]
        val packet = createPacket(packetId)

        if(packet == null){
            Console.warnLog("Packet $packetId doesnt exist, closing connection")
            ctx.close()
            return
        }

        try {
            packet.decode(data)
            packet.resolve(ctx.channel())
        } catch (ex: Exception){
            Console.errLog("Error processing packet ${ex.message}")
            ctx.close()
        }
    }

    internal fun sendCPEHandshake(player: Player){
        val count = supportedCPE.size.toShort()
        ServerExtInfo(Server.getSoftware(),count).send(player)
    }
    internal fun handleCPEHandshake(info: ClientExtInfo, channel: Channel){
        val count = supportedCPE.size.toShort()
        val player = Players.getConnecting(channel)
        if(player == null){
            return
        }
        if(info.extensionCount < count){
            player.kick("Mismatching CPE support count")
            return
        }

        player.supportedCpeCount = info.extensionCount
        player.client = info.appName
    }

    // Handles received CPE from player
    internal fun handleCPEEntry(extEntry: ClientExtEntry, channel: Channel){
        val player = Players.getConnecting(channel)
        if(player == null){
            return
        }

        val extName = extEntry.extName
        val version = extEntry.version

        player.addCPE(extName, version)

        if(player.getCPE().size.toShort() == player.supportedCpeCount){
            handleClientPostCPE(player)
            Players.finalizeHandshake(player)
        }
    }
    // Sends supportedCPE
    internal fun sendCPEEntries(player: Player){
        supportedCPE.forEach { (extName, version) ->
            ServerExtEntry(extName, version).send(player)
        }
    }

    //handle client postCPe and before ServerInfo
    internal fun handleClientPostCPE(player: Player){
        if(player.supports("CustomBlocks")){
            ServerCustomBlockLevel(1).send(player)
        }
    }


    /**
     * Adds a supported CPE extension by name and version.
     * @param name The name of the CPE extension.
     * @param version The version of the CPE extension.
     */
    fun addCPE(name: String, version: Int) {
        if (!supportedCPE.any { it.first == name && it.second == version }) {
            supportedCPE.add(name to version)
        }
    }

    /**
     * Adds a supported CPE extension by name only (defaults to version 0).
     * @param name The name of the CPE extension.
     */
    fun addCPE(name: String) {
        addCPE(name, 1)
    }

    /**
     * Checks if a CPE extension is supported (optionally checks version).
     * @param name The name of the CPE extension.
     * @param version The version to check (optional).
     * @return true if supported, false otherwise.
     */
    fun supports(name: String, version: Int? = null): Boolean {
        return if (version == null) {
            supportedCPE.any { it.first == name }
        } else {
            supportedCPE.any { it.first == name && it.second == version }
        }
    }

    /**
     * Returns the list of supported CPE extensions as <name, version> pairs.
     */
    fun getSupportedCPE(): List<Pair<String, Int>> = supportedCPE.toList()
}