package org.dandelion.classic.player

import io.netty.channel.Channel
import org.dandelion.classic.types.Position

data class Player(
    val channel: Channel,
    val client: String,

    val name: String,
    val position: Position,

    var id: Byte = -1,
    var isOp: Boolean = false,
){
    fun sendMessage(message: String){

    }
    fun kick(reason: String = "you have been kicked"){

    }
    fun ban(reason: String = "you have been banned from this server"){

    }
    fun setPosition(x: Int, y: Int, z: Int){
        setPosition(x.toFloat(), y.toFloat(), z.toFloat(), position.yaw, position.pitch)
    }
    fun setPosition(x: Float, y: Float, z: Float){
        setPosition(x, y, z, position.yaw, position.pitch)
    }
    fun setPosition(x: Float, y: Float, z: Float, yaw: Float, pitch: Float){
        position.set(x, y, z, yaw, pitch)
        //do tp logic here
    }
    fun setRotation(yaw: Float, pitch: Float){
        setPosition(position.x, position.y, position.z, yaw, pitch)
    }
}
