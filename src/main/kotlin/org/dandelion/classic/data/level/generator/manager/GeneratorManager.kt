package org.dandelion.classic.server.data.level.generator.manager

import org.dandelion.classic.server.data.level.generator.impl.FlatRoom
import org.dandelion.classic.server.data.level.generator.model.LevelGenerator
import org.dandelion.classic.server.util.Logger

object GeneratorManager {
    private val generators = mutableMapOf<String, LevelGenerator>()

    init {
        register("flat_room", FlatRoom())
    }

    fun register(name: String, generator: LevelGenerator) {
        generators[name] = generator
        Logger.infoLog("Registered generator: $name")
    }

    fun get(name: String): LevelGenerator? = generators[name]

    fun unregister(name: String) {
        if (generators.remove(name) != null) {
            Logger.infoLog("Unregistered generator: $name")
        } else {
            Logger.warnLog("Generator $name not found for unregistration")
        }
    }
}

