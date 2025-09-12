package org.dandelion.server.models

import org.dandelion.server.entity.player.Player
import org.dandelion.server.entity.player.PlayerRegistry
import org.dandelion.server.server.Console
import org.dandelion.server.types.vec.FVec
import java.io.File

//this model system was based on zoey's

object ModelRegistry {
    private val models = HashMap<UByte, Model>()
    private var nextModelId: UByte = 1u
    private const val MAX_MODELS = 64

    fun init() {
        loadAllFromDirectory("models")
    }

    fun loadAllFromDirectory(directoryPath: String) {
        val directory = File(directoryPath)
        if (!directory.exists()) {
            directory.mkdirs()
            return
        }

        if (!directory.isDirectory) {
            Console.warnLog("Path '$directoryPath' is not a directory")
            return
        }

        loadModelsFromDirectory(directory)
    }

    private fun loadModelsFromDirectory(directory: File) {
        val modelFiles = directory.listFiles { file ->
            file.isFile && file.extension.equals("bbmodel", ignoreCase = true)
        } ?: return

        modelFiles.forEach { file ->
            Console.log("Processing model: ${file.nameWithoutExtension}")
            val model = createModelFromFile(file)
            if (model != null) {
                registerModel(model)
                Console.log("Model loaded: ${model.name}")
            } else {
                Console.warnLog("Failed to load model: ${file.nameWithoutExtension}")
            }
        }
    }

    fun createModelFromFile(bbmodelFile: File): Model? {
        return try {
            if (!bbmodelFile.exists()) return null

            val modelName = bbmodelFile.nameWithoutExtension
            val modelId = getNextAvailableId() ?: return null

            val config = ModelConfigurationManager.ensureConfigurationExists(modelName)
            val jsonConfig = org.dandelion.server.util.JsonConfig.load(bbmodelFile)
            val parts = BlockBenchParser.parsePartsFromConfig(jsonConfig)

            if (parts.isEmpty()) return null

            val textureWidth = jsonConfig.getInt("resolution.width") ?: 64
            val textureHeight = jsonConfig.getInt("resolution.height") ?: 64

            config.calculateIdealConfiguration(parts)
            ModelConfigurationManager.saveConfiguration(config)

            var flags: UByte = 0u
            if (config.bobbing) flags = flags or Model.FLAG_BOBBING
            if (config.pushes) flags = flags or Model.FLAG_PUSHES
            if (config.usesHumanSkin) flags = flags or Model.FLAG_USES_HUMAN_SKIN
            if (config.calcHumanAnims) flags = flags or Model.FLAG_CALC_HUMAN_ANIMS

            Model(
                id = modelId,
                name = config.getFullName(),
                flags = flags,
                nameY = config.nameY / 16.0f,
                eyeY = config.eyeY / 16.0f,
                collisionSize = FVec(
                    config.collisionBounds.x / 16.0f,
                    config.collisionBounds.y / 16.0f,
                    config.collisionBounds.z / 16.0f
                ),
                pickingBoundsMin = FVec(
                    config.pickingBoundsMin.x / 16.0f,
                    config.pickingBoundsMin.y / 16.0f,
                    config.pickingBoundsMin.z / 16.0f
                ),
                pickingBoundsMax = FVec(
                    config.pickingBoundsMax.x / 16.0f,
                    config.pickingBoundsMax.y / 16.0f,
                    config.pickingBoundsMax.z / 16.0f
                ),
                uScale = textureWidth.toUShort(),
                vScale = textureHeight.toUShort(),
                parts = parts
            )
        } catch (e: Exception) {
            Console.errLog("Failed to create model from file ${bbmodelFile.name}: ${e.message}")
            null
        }
    }

    fun registerModel(model: Model): Boolean {
        if (models.containsKey(model.id)) {
            Console.warnLog("Model with ID ${model.id} is already registered")
            return false
        }

        if (models.size >= MAX_MODELS) {
            Console.warnLog("Cannot register model - maximum of $MAX_MODELS models reached")
            return false
        }

        models[model.id] = model
        PlayerRegistry.getAll().forEach { player -> model.sendToPlayer(player) }
        return true
    }

    fun unregisterModel(modelId: UByte): Boolean {
        return models.remove(modelId) != null
    }

    fun getModel(modelId: UByte): Model? = models[modelId]

    fun getModelByName(name: String): Model? =
        models.values.find { it.name.equals(name, ignoreCase = true) }

    fun getAllModels(): List<Model> = models.values.toList()

    fun isModeRegistered(modelId: UByte): Boolean = models.containsKey(modelId)

    fun getModelCount(): Int = models.size

    fun getNextAvailableId(): UByte? {
        if (models.size >= MAX_MODELS) return null

        while (models.containsKey(nextModelId)) {
            nextModelId = ((nextModelId.toInt() + 1) % MAX_MODELS).toUByte()
            if (nextModelId == 0.toUByte()) nextModelId = 1u
        }
        return nextModelId
    }

    fun sendAllModelsToPlayer(player: Player) {
        models.values.forEach { model ->
            model.sendToPlayer(player)
        }
    }
}