package org.dandelion.classic.data.level.generator.impl

import org.dandelion.classic.data.level.model.Level
import org.dandelion.classic.data.level.generator.model.LevelGenerator
import org.dandelion.classic.util.Logger
import org.json.JSONObject

class Fractal : LevelGenerator {
    override val id: String = "fractal"

    override fun generate(level: Level, params: String) {
        Logger.log("Generating fractal level in ${level.id} with params: $params")
        val paramsObj = if (params.isNotBlank()) JSONObject(params) else JSONObject()
        val mode = paramsObj.optString("mode", "m-sponge")
        val blockId = paramsObj.optInt("blockId", 1).toByte()
        val stack = paramsObj.optBoolean("stack", false)
        val size = minOf(level.sizeX, level.sizeY, level.sizeZ).toInt()
        val fractalSize = Integer.highestOneBit(size)
        val offsetX = (level.sizeX - fractalSize) / 2
        val offsetY = (level.sizeY - fractalSize) / 2
        val offsetZ = (level.sizeZ - fractalSize) / 2

        fun fillFractalAt(x: Int, y: Int, z: Int) {
            when (mode) {
                "s-pyramid" -> sPyramid(level, x, y, z, fractalSize, blockId)
                else -> mSponge(level, x, y, z, fractalSize, blockId)
            }
        }

        if (stack) {
            val countX = level.sizeX / fractalSize
            val countY = level.sizeY / fractalSize
            val countZ = level.sizeZ / fractalSize
            for (cx in 0 until countX) {
                for (cy in 0 until countY) {
                    for (cz in 0 until countZ) {
                        fillFractalAt(cx * fractalSize, cy * fractalSize, cz * fractalSize)
                    }
                }
            }
        } else {
            fillFractalAt(offsetX, offsetY, offsetZ)
        }
        Logger.log("Fractal generated in ${level.id}")
    }

    private fun sPyramid(level: Level, ox: Int, oy: Int, oz: Int, size: Int, blockId: Byte) {
        fun draw(x: Int, y: Int, z: Int, s: Int) {
            if (s < 1) return
            if (s == 1) {
                level.setBlock(x, y, z, blockId)
                return
            }
            val half = s / 2
            draw(x, y, z, half)
            draw(x + half, y, z, half)
            draw(x, y, z + half, half)
            draw(x + half, y, z + half, half)
            draw(x + half / 2, y + half, z + half / 2, half)
        }
        draw(ox, oy, oz, size)
    }

    private fun mSponge(level: Level, ox: Int, oy: Int, oz: Int, size: Int, blockId: Byte) {
        val validSize = nearestPowerOf(size, 3)
        if (validSize != size) {
            Logger.warnLog("Menger Sponge only works with sizes that are powers of 3 (e.g., 27, 81, 243, 729, ...). Using nearest valid size: $validSize")
        }
        fun draw(x: Int, y: Int, z: Int, s: Int) {
            if (s < 1) return
            if (s == 1) {
                level.setBlock(x, y, z, blockId)
                return
            }
            val third = s / 3
            for (dx in 0 until 3) for (dy in 0 until 3) for (dz in 0 until 3) {
                val cnt = listOf(dx, dy, dz).count { it == 1 }
                if (cnt >= 2) continue
                draw(x + dx * third, y + dy * third, z + dz * third, third)
            }
        }
        draw(ox, oy, oz, validSize)
    }

    private fun nearestPowerOf(n: Int, base: Int): Int {
        if (n < 1) return 1
        var lower = 1
        while (lower * base <= n) lower *= base
        val upper = lower * base
        return if (n - lower < upper - n) lower else upper
    }

    private fun isPowerOf(n: Int, base: Int): Boolean {
        if (n < 1) return false
        var value = n
        while (value % base == 0) {
            value /= base
        }
        return value == 1
    }
}
