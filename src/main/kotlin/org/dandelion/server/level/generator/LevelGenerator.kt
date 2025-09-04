package org.dandelion.server.level.generator

import org.dandelion.server.level.Level

interface LevelGenerator {
    val id: String

    val author: String
    val description: String

    fun generate(level: Level, params: String)
}
