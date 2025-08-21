package org.dandelion.classic.types.vec

import kotlin.math.sqrt

data class IVec(var x: Int, var y: Int, var z: Int) {
    fun distanceTo(other: IVec): Double {
        val dx = (x - other.x).toDouble()
        val dy = (y - other.y).toDouble()
        val dz = (z - other.z).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    operator fun plus(other: IVec): IVec {
        return IVec(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: IVec): IVec {
        return IVec(x - other.x, y - other.y, z - other.z)
    }

    operator fun times(scalar: Int): IVec {
        return IVec(x * scalar, y * scalar, z * scalar)
    }

    operator fun div(scalar: Int): IVec {
        return IVec(x / scalar, y / scalar, z / scalar)
    }

    fun dot(other: IVec): Int {
        return x * other.x + y * other.y + z * other.z
    }

    fun cross(other: IVec): IVec {
        return IVec(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x,
        )
    }
}
