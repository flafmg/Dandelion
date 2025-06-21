package org.dandelion.classic.level.io

import org.dandelion.classic.level.Level
import java.io.File

interface LevelSerializer {
    fun serialize(level: Level, file: File)
}

interface LevelDeserializer {
    fun deserialize(file: File): Level?
}
