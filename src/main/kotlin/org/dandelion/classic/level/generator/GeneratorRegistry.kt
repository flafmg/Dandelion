package org.dandelion.classic.level.generator

import org.dandelion.classic.level.generator.impl.FlatGenerator
import org.dandelion.classic.server.Console

object GeneratorRegistry {
    val generators = HashMap<String, LevelGenerator>()

    internal fun init(){
        register(FlatGenerator())
    }
    internal fun shutdown(){
        unregister(FlatGenerator())
    }

    fun register(generator: LevelGenerator){
        if(generators.containsKey(generator.id)){
            Console.warnLog("A generator by the same id is already registered")
            return
        }
        Console.log("registered generator ${generator.id}")
        generators[generator.id] = generator
    }

    fun unregister(generator: LevelGenerator){
        unregister(generator.id)
    }
    fun unregister(id: String){
        if(!generators.containsKey(id)){
            Console.warnLog("This generator doesnt exist")
            return
        }
        generators.remove(id)
    }

    fun getGenerator(id: String): LevelGenerator?{
        if(!generators.containsKey(id)){
            Console.warnLog("This generator doesnt exist")
            return null
        }
        return generators[id]
    }
    fun getAllGenerators(): List<LevelGenerator> = generators.values.toList()

}