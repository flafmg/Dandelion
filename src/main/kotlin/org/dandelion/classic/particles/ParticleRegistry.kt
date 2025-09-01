package org.dandelion.classic.particles

import org.dandelion.classic.entity.player.Player
import org.dandelion.classic.entity.player.Players
import org.dandelion.classic.network.packets.cpe.server.ServerDefineEffect
import org.dandelion.classic.network.packets.cpe.server.ServerSpawnEffect
import org.dandelion.classic.types.Position

object ParticleRegistry {
    private val registry: Array<Particle?> = arrayOfNulls(256)

    @JvmStatic
    fun addParticle(particle: Particle): Boolean {
        val definedCount = registry.count { it != null }
        if (definedCount >= 255) return false

        val freeIndex = registry.indexOfFirst { it == null }
        if (freeIndex == -1) return false

        particle.effectId = freeIndex.toByte()
        registry[freeIndex] = particle

        defineParticle(particle)

        return true
    }

    @JvmStatic
    fun removeParticle(effectId: Byte): Boolean {
        val index = effectId.toInt() and 0xFF

        val present = registry[index] != null
        registry[index] = null

        return present
    }

    @JvmStatic
    fun getParticle(effectId: Byte): Particle? = registry[effectId.toInt() and 0xFF]

    private fun defineParticle(particle: Particle) {
        val red = (particle.tint.red.toInt() and 0xFF).toByte()
        val green = (particle.tint.green.toInt() and 0xFF).toByte()
        val blue = (particle.tint.blue.toInt() and 0xFF).toByte()

        val sizeVariation = (particle.sizeVariation * 100f).toDouble().toLong().coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        val gravity = (particle.gravity * 10000f).toDouble().toLong().coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        val baseLifetime = (particle.baseLifetime * 10000f).toDouble().toLong().coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        val lifetimeVariation = (particle.lifetimeVariation * 100f).toDouble().toLong().coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

        val collideFlags = particle.collisionFlags.toByte()
        val fullBright: Byte = if (particle.fullBright) 1 else 0

        val packet = ServerDefineEffect(
            effectId = particle.effectId,
            u1 = particle.u1,
            v1 = particle.v1,
            u2 = particle.u2,
            v2 = particle.v2,
            redTint = red,
            greenTint = green,
            blueTint = blue,
            frameCount = particle.frameCount,
            particleCount = particle.particleCount,
            particleSize = particle.size,
            sizeVariation = sizeVariation,
            spread = particle.spread,
            speed = particle.speed,
            gravity = gravity,
            baseLifetime = baseLifetime,
            lifetimeVariation = lifetimeVariation,
            collideFlags = collideFlags,
            fullBright = fullBright,
        )

        Players.getAllPlayers().forEach { packet.send(it) }
    }

    @JvmStatic
    fun spawnParticleFor(player: Player, particle: Particle, location: Position, origin: Position) {
        val px = (location.x * 32f).toInt()
        val py = (location.y * 32f).toInt()
        val pz = (location.z * 32f).toInt()
        val ox = (origin.x * 32f).toInt()
        val oy = (origin.y * 32f).toInt()
        val oz = (origin.z * 32f).toInt()

        val packet = ServerSpawnEffect(
            effectId = particle.effectId,
            positionX = px,
            positionY = py,
            positionZ = pz,
            originX = ox,
            originY = oy,
            originZ = oz,
        )

        packet.send(player)
    }

    @JvmStatic
    fun spawnParticleFor(player: Player, effectId: Byte, location: Position, origin: Position) {
        val particle = getParticle(effectId) ?: return
        spawnParticleFor(player, particle, location, origin)
    }
}