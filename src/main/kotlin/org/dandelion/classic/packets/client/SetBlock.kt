package org.dandelion.classic.server.packets.client

import org.dandelion.classic.server.data.level.manager.LevelManager
import org.dandelion.classic.server.data.player.manager.PlayerManager
import org.dandelion.classic.server.packets.model.Packet
import org.dandelion.classic.server.packets.stream.PacketReader
import io.netty.channel.Channel
import org.dandelion.classic.server.events.packetEvents.manager.PacketEventManager

class SetBlock : Packet() {
    override val id: Byte = 0x05
    override val size = 9

    var x: Short = 0
    var y: Short = 0
    var z: Short = 0
    var mode: Byte = 0
    var blockType: Byte = 0

    override fun decode(data: ByteArray) {
        val reader = PacketReader(data)
        x = reader.readShort()
        y = reader.readShort()
        z = reader.readShort()
        mode = reader.readByte()
        blockType = reader.readByte()
    }

    override fun resolve(channel: Channel) {
        if (!PacketEventManager.fireReceive(this, channel)) return
        val player = PlayerManager.getPlayerByChannel(channel)?: return
        LevelManager.setBlock(player.levelId, x, y, z, blockType, mode)
    }
}
