package org.dandelion.classic.data.level.generator.impl

import org.dandelion.classic.data.level.model.Level
import org.dandelion.classic.data.level.generator.model.LevelGenerator
import org.dandelion.classic.util.Logger
import org.json.JSONObject
import kotlinx.coroutines.runBlocking
import kotlin.math.min

class Fractal : LevelGenerator {
    override fun generate(level: Level, params: String) {
        Logger.log("Generating fractal in ${level.id} with params: $params")
        val paramsObj = if (params.isNotBlank()) JSONObject(params) else JSONObject()
        val mode = paramsObj.optString("mode", "s-cube")

        val stack = paramsObj.optBoolean("stack", false)
        val size = minOf(level.sizeX, level.sizeY, level.sizeZ).toInt()

        val fractalSize = Integer.highestOneBit(size)

        val offsetX = (level.sizeX - fractalSize) / 2
        val offsetY = (level.sizeY - fractalSize) / 2
        val offsetZ = (level.sizeZ - fractalSize) / 2

        fun fillFractalAt(baseX: Int, baseY: Int, baseZ: Int) {
            when (mode) {
                "t-square" -> tSquare(level, baseX, baseY, baseZ, fractalSize)
                "s-pyramid" -> sPyramid(level, baseX, baseY, baseZ, fractalSize)
                else -> sCube(level, baseX, baseY, baseZ, fractalSize)
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

    private fun tSquare(level: Level, ox: Int, oy: Int, oz: Int, size: Int) {
        fun draw(x: Int, y: Int, z: Int, s: Int) {
            if (s < 1) return
            val half = s / 2
            for (i in x until x + s) {
                for (j in z until z + s) {
                    runBlocking { level.setBlock(i, y, j, 1) }
                }
            }
            if (half > 0) {
                draw(x, y, z, half)
                draw(x + half, y, z, half)
                draw(x, y, z + half, half)
                draw(x + half, y, z + half, half)
            }
        }
        draw(ox, oy + size / 2, oz, size)
    }

    private fun sPyramid(level: Level, ox: Int, oy: Int, oz: Int, size: Int) {
        fun draw(x: Int, y: Int, z: Int, s: Int) {
            if (s < 1) return
            if (s == 1) {
                runBlocking { level.setBlock(x, y, z, 2) }
                return
            }
            val half = s / 2
            draw(x, y, z, half)
            draw(x + half, y, z, half)
            draw(x + half / 2, y + half, z + half / 2, half)
            draw(x, y, z + half, half)
            draw(x + half, y, z + half, half)
        }
        draw(ox, oy, oz, size)
    }

    private fun sCube(level: Level, ox: Int, oy: Int, oz: Int, size: Int) {
        fun draw(x: Int, y: Int, z: Int, s: Int) {
            if (s < 1) return
            if (s == 1) {
                runBlocking { level.setBlock(x, y, z, 3) }
                return
            }
            val half = s / 2
            for (dx in 0..1) for (dy in 0..1) for (dz in 0..1) {
                if (dx + dy + dz < 2) {
                    draw(x + dx * half, y + dy * half, z + dz * half, half)
                }
            }
        }
        draw(ox, oy, oz, size)
    }
}

