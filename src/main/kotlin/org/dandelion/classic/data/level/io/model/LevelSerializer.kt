package org.dandelion.classic.server.data.level.io.model

import org.dandelion.classic.server.data.level.model.Level

interface LevelSerializer {
    fun serialize(level: Level): ByteArray
}

interface LevelDeserializer {
    fun deserialize(data: ByteArray, id: String): Level
}

