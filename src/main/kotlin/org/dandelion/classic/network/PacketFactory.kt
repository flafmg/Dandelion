package org.dandelion.classic.network

import io.netty.channel.ChannelHandlerContext
import org.dandelion.classic.network.packets.Packet
import org.dandelion.classic.network.packets.classic.client.ClientIdentification
import org.dandelion.classic.network.packets.classic.client.ClientMessage
import org.dandelion.classic.network.packets.classic.client.ClientPositionAndOrientation
import org.dandelion.classic.network.packets.classic.client.ClientSetBlock
import org.dandelion.classic.server.Console
import java.util.concurrent.ConcurrentHashMap

//is it correct to call this a factory?
object PacketFactory {
    private val packetFactory = ConcurrentHashMap<Byte, () -> Packet>()
    private val supportedCPE = mutableListOf<Byte>()

    internal fun init() {
        Console.log("initializing packet handler...")
        registerPackets()
        registerSupportedCPE()
    }

    private fun registerPackets(){
        registerPacket(0x00, ::ClientIdentification)
        registerPacket(0x05, ::ClientSetBlock)
        registerPacket(0x08, ::ClientPositionAndOrientation)
        registerPacket(0x0D, ::ClientMessage)
    }
    private fun registerSupportedCPE(){

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

    //this is made public in case this software dies community can still maintain CPE support

    /**
     * Adds a supported CPE by its byte id.
     * @param id The byte id of the CPE to add.
     */
    fun addSupportedCPE(id: Byte) {
        if (!supportedCPE.contains(id)) {
            supportedCPE.add(id)
        }
    }

    /**
     * Gets a ByteArray of all supported CPE extension ids.
     * @return ByteArray of supported CPE ids.
     */
    fun getSupportedCPE(): ByteArray = supportedCPE.toByteArray()
}