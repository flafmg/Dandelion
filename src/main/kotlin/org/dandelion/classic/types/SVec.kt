package org.dandelion.classic.types

data class SVec(var x: Short, var y: Short, var z: Short) {
    fun distanceTo(other: SVec): Double {
        val dx = (x - other.x).toDouble()
        val dy = (y - other.y).toDouble()
        val dz = (z - other.z).toDouble()
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    operator fun plus(other: SVec): SVec {
        return SVec(
            (x + other.x).toShort(),
            (y + other.y).toShort(),
            (z + other.z).toShort(),
        )
    }

    operator fun minus(other: SVec): SVec {
        return SVec(
            (x - other.x).toShort(),
            (y - other.y).toShort(),
            (z - other.z).toShort(),
        )
    }

    operator fun times(scalar: Short): SVec {
        return SVec(
            (x * scalar).toShort(),
            (y * scalar).toShort(),
            (z * scalar).toShort(),
        )
    }

    operator fun div(scalar: Short): SVec {
        return SVec(
            (x / scalar).toShort(),
            (y / scalar).toShort(),
            (z / scalar).toShort(),
        )
    }

    fun dot(other: SVec): Int {
        return x * other.x + y * other.y + z * other.z
    }

    fun cross(other: SVec): SVec {
        return SVec(
            (y * other.z - z * other.y).toShort(),
            (z * other.x - x * other.z).toShort(),
            (x * other.y - y * other.x).toShort(),
        )
    }
}
