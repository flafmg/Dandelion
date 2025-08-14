package org.dandelion.classic.level.generator

import org.dandelion.classic.level.generator.impl.FlatGenerator
import org.dandelion.classic.server.Console

object GeneratorRegistry {
    val generators = HashMap<String, LevelGenerator>()

    internal fun init() {
        register(FlatGenerator())
    }

    @JvmStatic
    fun register(generator: LevelGenerator) {
        if (generators.containsKey(generator.id)) {
            Console.warnLog("A generator by the same id is already registered")
            return
        }
        Console.log("registered generator ${generator.id}")
        generators[generator.id] = generator
    }

    @JvmStatic
    fun unregister(generator: LevelGenerator) {
        unregister(generator.id)
    }

    @JvmStatic
    fun unregister(id: String) {
        if (!generators.containsKey(id)) {
            Console.warnLog("This generator doesnt exist")
            return
        }
        generators.remove(id)
    }

    @JvmStatic
    fun getGenerator(id: String): LevelGenerator? {
        if (!generators.containsKey(id)) {
            Console.warnLog("This generator doesnt exist")
            return null
        }
        return generators[id]
    }

    @JvmStatic
    fun getAllGenerators(): List<LevelGenerator> = generators.values.toList()
}
