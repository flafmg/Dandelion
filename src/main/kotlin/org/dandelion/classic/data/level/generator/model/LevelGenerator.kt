package org.dandelion.classic.server.data.level.generator.model

import org.dandelion.classic.server.data.level.model.Level

interface LevelGenerator {
    fun generate(level: Level, params: String = "")
}