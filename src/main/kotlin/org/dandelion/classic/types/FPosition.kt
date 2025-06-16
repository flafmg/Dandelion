package org.dandelion.classic.types

import kotlin.math.pow
import kotlin.math.sqrt

data class FPosition(
    var x: Float,
    var y: Float,
    var z: Float,
    var yaw: Float,
    var pitch: Float
){
    fun set(x: Float, y: Float, z: Float, yaw: Float, pitch: Float){
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }
    fun distanceTo(target: FPosition): Float {
        return sqrt(
            (x - target.x).toDouble().pow(2.0)
            + (y - target.y).toDouble().pow(2.0)
            + (z - target.z).toDouble().pow(2.0)
        ).toFloat()
    }
}
