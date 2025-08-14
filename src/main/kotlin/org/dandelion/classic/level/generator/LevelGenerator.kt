package org.dandelion.classic.level.generator

import org.dandelion.classic.level.Level

interface LevelGenerator {
    val id: String

    val author: String
    val description: String

    fun generate(level: Level, params: String)
}
