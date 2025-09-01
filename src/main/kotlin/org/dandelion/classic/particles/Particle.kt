package org.dandelion.classic.particles

import org.dandelion.classic.types.extensions.Color

enum class ExpirationPolicy(val bit: Boolean) {
    EXPIRE_ON_WALL_CEILING_ONLY(false),
    EXPIRE_ON_ANY_COLLISION(true),
}

data class ParticleCollisionFlags(
    val expirationPolicy: ExpirationPolicy,
    val collideSolidIce: Boolean,
    val collideWaterLavaRope: Boolean,
    val collideLeafDraw: Boolean,
) {
    fun toByte(): Byte {
        var value = 0
        if (expirationPolicy.bit) value = value or (1 shl 7)
        if (collideSolidIce) value = value or (1 shl 6)
        if (collideWaterLavaRope) value = value or (1 shl 5)
        if (collideLeafDraw) value = value or (1 shl 4)
        return value.toByte()
    }

    companion object {
        fun fromByte(byte: Byte): ParticleCollisionFlags {
            val b = byte.toInt()
            return ParticleCollisionFlags(
                expirationPolicy =
                    if ((b and (1 shl 7)) != 0)
                        ExpirationPolicy.EXPIRE_ON_ANY_COLLISION
                    else ExpirationPolicy.EXPIRE_ON_WALL_CEILING_ONLY,
                collideSolidIce = (b and (1 shl 6)) != 0,
                collideWaterLavaRope = (b and (1 shl 5)) != 0,
                collideLeafDraw = (b and (1 shl 4)) != 0,
            )
        }
    }
}

data class Particle(
    val u1: Byte,
    val v1: Byte,
    val u2: Byte,
    val v2: Byte,
    val tint: Color,
    val frameCount: Byte,
    val particleCount: Byte,
    val size: Byte,
    val sizeVariation: Float,
    val spread: UShort,
    val speed: Int,
    val gravity: Float,
    val baseLifetime: Float,
    val lifetimeVariation: Float,
    val collisionFlags: ParticleCollisionFlags,
    val fullBright: Boolean,
    val positionX: Int,
    val positionY: Int,
    val positionZ: Int,
    val originX: Int,
    val originY: Int,
    val originZ: Int,
){
    var effectId: Byte = 0
        internal set
}
