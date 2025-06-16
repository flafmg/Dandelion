package org.dandelion.classic.player

import io.netty.channel.Channel
import org.dandelion.classic.types.FPosition

data class Player(
    val channel: Channel,
    val client: String,

    val name: String,
    val position: FPosition,

    var isOp: Boolean = false,
){
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
