package org.dandelion.classic.server.data.level.generator.impl

import org.dandelion.classic.server.data.level.model.Level
import org.dandelion.classic.server.data.level.generator.model.LevelGenerator
import org.dandelion.classic.server.util.Logger
import org.json.JSONObject
import kotlinx.coroutines.runBlocking

class FlatRoom : LevelGenerator {
    override fun generate(level: Level, params: String) {
        Logger.log("Generating ${level.id} as flat room with params: $params")
        val paramsObj = if (params.isNotBlank()) JSONObject(params) else JSONObject()
        val sizeY = level.sizeY.toInt()
        val layers = mutableMapOf<Int, Byte>()
        if (paramsObj.has("layer")) {
            val layerObj = paramsObj.getJSONObject("layer")
            for (key in layerObj.keys()) {
                layers[key.toInt()] = layerObj.getInt(key).toByte()
            }
        } else {
            val mid = sizeY / 2
            layers[sizeY - 1] = 0
            layers[mid] = 2
            layers[mid - 1] = 3
            layers[mid - 3] = 1
        }
        val sorted = layers.toSortedMap(compareByDescending { it })
        val yList = sorted.keys.sortedDescending()
        val blockForY = ByteArray(sizeY) { 0 }
        var lastBlock: Byte = 0
        var idx = 0
        for (y in sizeY - 1 downTo 0) {
            while (idx < yList.size && y < yList[idx]) idx++
            lastBlock = if (idx < yList.size && y == yList[idx]) sorted[yList[idx]]!! else lastBlock
            blockForY[y] = lastBlock
        }
        for (z in 0 until level.sizeZ) {
            for (y in 0 until sizeY) {
                val block = blockForY[y]
                for (x in 0 until level.sizeX) {
                    runBlocking { level.setBlock(x, y, z, block) }
                }
            }
        }
        Logger.log("Level ${level.id} generated")
    }
}

