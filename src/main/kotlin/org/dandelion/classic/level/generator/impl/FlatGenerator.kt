package org.dandelion.classic.level.generator.impl

import org.dandelion.classic.level.Level
import com.google.gson.GsonBuilder
import org.dandelion.classic.level.generator.LevelGenerator
import org.dandelion.classic.types.Block

data class Layer(
    val id: Byte,
    val count: Int
)
data class FlatGeneratorParams(
    val layers: List<Layer>?
)
class FlatGenerator : LevelGenerator {
    override val id: String = "flat"
    override val author: String = "Dandelion"
    override val description: String = "A flat level generator"

    private val gson = GsonBuilder().setPrettyPrinting().create()
    override fun generate(level: Level, params: String) {
        val generatorParams = parseParams(params) ?: getDefaultParams(level)

        val layers = generatorParams.layers ?: getDefaultParams(level).layers!!
        var currentY = 0
        for(layer in layers){
            val endY = minOf(currentY + layer.count - 1, level.size.y - 1)

            level.fillBlocks(0, currentY, 0, level.size.x -1, endY, level.size.z - 1, layer.id)
            currentY += layer.count
        }
    }
    private fun parseParams(jsonString: String) : FlatGeneratorParams?{
        return gson.fromJson(jsonString, FlatGeneratorParams::class.java)
    }
    private fun getDefaultParams(level: Level): FlatGeneratorParams {
        val halfY = level.size.y / 2
        val stoneCount = halfY - 1
        val grassCount = 1

        val defaultParams = FlatGeneratorParams(
            layers =  listOf(
                Layer(Block.Stone.id, stoneCount),
                Layer(Block.GrassBlock.id, grassCount)
            )
        )
        return defaultParams
    }
}