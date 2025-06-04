package org.dandelion.classic.data.level.model

import org.dandelion.classic.Console
import org.dandelion.classic.data.player.model.Player
import org.dandelion.classic.data.level.io.model.LevelSerializer
import org.dandelion.classic.data.level.io.model.LevelDeserializer

data class Level(
    val id: String = "none",
    val sizeX: Short = 128,
    val sizeY: Short = 64,
    val sizeZ: Short = 128,

    var spawnX: Float = 32.0f,
    var spawnY: Float = 16.0f,
    var spawnZ: Float = 32.0f,

    val seed: Long = 0,

    var blocks: ByteArray = ByteArray(0),

    var autoSaveInterval: Int = 900
) {
    var players: Array<Player?> = Array(256) { null }

    init {
        if (blocks.isEmpty()) {
            val volume = sizeX.toInt() * sizeY.toInt() * sizeZ.toInt()
            blocks = ByteArray(volume)
            blocks.fill(0)
        }
    }

    fun getBlock(x: Int, y: Int, z: Int): Byte {
        return blocks[x + sizeX * (z + sizeZ * y)]
    }

    fun setBlock(x: Int, y: Int, z: Int, block: Byte) {
        blocks[x + sizeX * (z + sizeZ * y)] = block
    }

    fun getPlayers(): List<Player> = players.filterNotNull()

    fun getPlayerById(id: Byte): Player? = players[id.toInt() and 0xFF]

    fun getFirstAvailableId(): Byte? {
        for (i in 0..254) {
            if (players[i] == null) return i.toByte()
        }
        return null
    }

    fun addPlayer(player: Player): Boolean {
        val id = getFirstAvailableId() ?: return false
        player.playerID = id
        players[id.toInt() and 0xFF] = player
        return true
    }

    fun removePlayer(id: Byte) {
        players[id.toInt() and 0xFF] = null
    }

    fun sendMessage(message: String, playerId: Byte = 0xff.toByte()) {
        players.filterNotNull().forEach { it.sendMessage(message, playerId) }
    }

    fun kickAll(reason: String = "Kicked by an operator") {
        players.filterNotNull().forEach { it.kick(reason) }
    }

    fun serialize(serializer: LevelSerializer, path: String) {
        serializer.serialize(this, path)
    }

    companion object {
        fun deserialize(path: String, deserializer: LevelDeserializer): Level {
            return deserializer.deserialize(path)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Level

        if (sizeX != other.sizeX) return false
        if (sizeY != other.sizeY) return false
        if (sizeZ != other.sizeZ) return false
        if (seed != other.seed) return false
        if (id != other.id) return false
        if (!blocks.contentEquals(other.blocks)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sizeX.toInt()
        result = 31 * result + sizeY.toInt()
        result = 31 * result + sizeZ.toInt()
        result = 31 * result + seed.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + blocks.contentHashCode()
        return result
    }
}
