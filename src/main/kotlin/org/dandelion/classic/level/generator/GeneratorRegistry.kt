package org.dandelion.classic.level.generator

import org.dandelion.classic.level.generator.impl.FlatGenerator

object GeneratorRegistry {
    val generators = HashMap<String, LevelGenerator>()

    internal fun init(){
        register(FlatGenerator())
    }
    internal fun shutDown(){
        unregister(FlatGenerator())
    }

    fun register(generator: LevelGenerator){
        if(generators.containsKey(generator.id)){
            println("A generator by the same id is already registered")
            return
        }
        println("registered generator ${generator.id}")
        generators[generator.id] = generator
    }

    fun unregister(generator: LevelGenerator){
        unregister(generator.id)
    }
    fun unregister(id: String){
        if(!generators.containsKey(id)){
            println("This generator doesnt exist")
            return
        }
        generators.remove(id)
    }

    fun getGenerator(id: String): LevelGenerator?{
        if(!generators.containsKey(id)){
            println("This generator doesnt exist")
            return null
        }
        return generators[id]
    }
    fun getAllGenerators(): List<LevelGenerator> = generators.values.toList()

}