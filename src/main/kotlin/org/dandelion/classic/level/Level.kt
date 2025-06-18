package org.dandelion.classic.level

import org.dandelion.classic.player.Player
import org.dandelion.classic.types.Position
import org.dandelion.classic.types.SVec

//TODO: add docstrings for clarity
data class Level(
    val id: String,

    val size: SVec,
    var spawn: Position,

    val seed: Int,
){
    var blocks: ByteArray = ByteArray(size.x * size.y * size.z) { 0x00 }

    val availableIds = ArrayDeque<Byte>(256) //we using it as LIFO (stack)
    val players = HashMap<Byte, Player>(256)

    init {
        for(id in 0..254){ // - 1 (255) is reserved for own player
            availableIds.addFirst(id.toByte())
        }
    }
    fun isFull(): Boolean {
        return availableIds.isEmpty()
    }
    fun getNextAvailableID(): Byte?{
        return availableIds.removeFirstOrNull()
    }
    fun getAvailableSpace(): Int{
        return availableIds.size
    }
    fun getPlayerCount(): Int{
        return players.size
    }

    fun setBlock(x: Int, y: Int, z: Int, block: Byte){
        val index = x + (z * size.x) + (y * size.x * size.z)
        blocks[index] = block
    }

    fun tryAddPlayer(player: Player): Boolean{
        val id = getNextAvailableID() ?: return false
        player.id = id
        return true
    }
    fun removePlayer(player: Player){
        if(!isPlayerInLevel(player)){
            println("Attempted to remove a player that isnt in this level")
        }
        removePlayer(player.id)
    }
    fun removePlayer(id: Byte){
        if(getPlayerById(id) == null){
            println("Attempted to remove a player that isnt in this level")
        }

        availableIds.addFirst(id)
        val player = players.remove(id)
        player?.id = -1
    }
    fun getPlayerById(id: Byte): Player?{
        return players[id]
    }
    fun isPlayerInLevel(player: Player): Boolean {
        return players.values.any { it === player }
    }
    fun kickAll(reason: String = "you have been kicked"){
        players.values.forEach{player -> player.kick(reason)}
    }
}
