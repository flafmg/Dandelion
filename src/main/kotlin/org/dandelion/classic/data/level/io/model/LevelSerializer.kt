package org.dandelion.classic.data.level.io.model

import org.dandelion.classic.data.level.model.Level

interface LevelSerializer {
    fun serialize(level: Level, path: String)
}

interface LevelDeserializer {
    fun deserialize(path: String): Level
}

