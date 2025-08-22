package org.dandelion.classic.types

import kotlin.math.pow
import kotlin.math.sqrt

data class Position(
    var x: Float,
    var y: Float,
    var z: Float,
    var yaw: Float = 0f,
    var pitch: Float = 0f,
) {
    constructor(
        x: Number,
        y: Number,
        z: Number,
        yaw: Float = 0f,
        pitch: Float = 0f,
    ) : this(x.toFloat(), y.toFloat(), z.toFloat(), yaw, pitch)

    fun set(x: Float, y: Float, z: Float, yaw: Float, pitch: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.yaw = yaw
        this.pitch = pitch
    }

    fun distanceTo(target: Position): Float {
        return sqrt(
                (x - target.x).toDouble().pow(2.0) +
                    (y - target.y).toDouble().pow(2.0) +
                    (z - target.z).toDouble().pow(2.0)
            )
            .toFloat()
    }

    // we should do yaw pitch too?
    operator fun plus(other: Position): Position {
        return Position(x + other.x, y + other.y, z + other.z, yaw, pitch)
    }

    operator fun minus(other: Position): Position {
        return Position(x - other.x, y - other.y, z - other.z, yaw, pitch)
    }

    operator fun times(scalar: Float): Position {
        return Position(x * scalar, y * scalar, z * scalar, yaw, pitch)
    }

    operator fun div(scalar: Float): Position {
        return Position(x / scalar, y / scalar, z / scalar, yaw, pitch)
    }

    fun clone(): Position {
        return Position(x, y, z, yaw, pitch)
    }

    override fun toString(): String {
        return "Position(x=$x, y=$y, z=$z, yaw=$yaw, pitch=$pitch)"
    }
}
