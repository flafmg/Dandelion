package org.dandelion.server.level.io

import java.io.File
import org.dandelion.server.level.Level

interface LevelSerializer {
    fun serialize(level: Level, file: File)
}

interface LevelDeserializer {
    fun deserialize(file: File): Level?
}
