package org.dandelion.classic.data.level.generator.model

import org.dandelion.classic.data.level.model.Level

interface LevelGenerator {
    val id: String;
    fun generate(level: Level, params: String = "")
}