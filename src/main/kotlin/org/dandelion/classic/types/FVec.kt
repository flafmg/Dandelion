package org.dandelion.classic.types

data class FVec(var x: Float, var y: Float, var z: Float) {
    fun distanceTo(other: FVec): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    operator fun plus(other: FVec): FVec {
        return FVec(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: FVec): FVec {
        return FVec(x - other.x, y - other.y, z - other.z)
    }

    operator fun times(scalar: Float): FVec {
        return FVec(x * scalar, y * scalar, z * scalar)
    }

    operator fun div(scalar: Float): FVec {
        return FVec(x / scalar, y / scalar, z / scalar)
    }

    fun dot(other: FVec): Float {
        return x * other.x + y * other.y + z * other.z
    }

    fun cross(other: FVec): FVec {
        return FVec(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x,
        )
    }
}
