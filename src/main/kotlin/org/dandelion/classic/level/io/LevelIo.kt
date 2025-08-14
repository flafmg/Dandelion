package org.dandelion.classic.level.io

import java.io.File
import org.dandelion.classic.level.Level

interface LevelSerializer {
    fun serialize(level: Level, file: File)
}

interface LevelDeserializer {
    fun deserialize(file: File): Level?
}
