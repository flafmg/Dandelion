package org.dandelion.classic.data.level.generator.manager

import org.dandelion.classic.data.level.generator.impl.FlatRoom
import org.dandelion.classic.data.level.generator.impl.Fractal
import org.dandelion.classic.data.level.generator.model.LevelGenerator
import org.dandelion.classic.util.Logger

object GeneratorManager {
    private val generators = mutableMapOf<String, LevelGenerator>()

    init {
        register(FlatRoom())
        register(Fractal())
    }

    fun register(generator: LevelGenerator) {
        generators[generator.id] = generator
        Logger.infoLog("Registered generator: ${generator.id}")
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

