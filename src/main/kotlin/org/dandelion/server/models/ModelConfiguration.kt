package org.dandelion.server.models

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.dandelion.server.server.Console
import org.dandelion.server.types.vec.FVec
import java.io.File
import kotlin.math.*

data class ModelConfiguration(
    var nameY: Float = 32.5f,
    var eyeY: Float = 26.0f,
    var collisionBounds: FVec = FVec(8.6f, 28.1f, 8.6f),
    var pickingBoundsMin: FVec = FVec(-8f, 0f, -4f),
    var pickingBoundsMax: FVec = FVec(8f, 32f, 4f),
    var bobbing: Boolean = true,
    var pushes: Boolean = true,
    var usesHumanSkin: Boolean = true,
    var calcHumanAnims: Boolean = true,
    var defaultSkin: String? = null
) {
    @Transient
    var modelName: String = ""
    @Transient
    var modifiers: MutableSet<String> = mutableSetOf()
    @Transient
    var scale: Float = 1.0f
    @Transient
    var fileName: String? = null

    constructor(name: String, overrideFileName: Boolean = false) : this() {
        parseModelName(name, overrideFileName)
    }

    private fun parseModelName(name: String, overrideFileName: Boolean) {
        val rawModel = getRawModel(name)
        this.scale = getRawScale(name)

        val split = rawModel.split("(", limit = 2)
        if (split.size == 2 && split[1].endsWith(")")) {
            if (overrideFileName || exists()) {
                this.fileName = rawModel
            }
            this.modelName = split[0]

            val attrs = split[1].dropLast(1)
            attrs.split(",").forEach { attr ->
                val trimmed = attr.trim()
                if (trimmed.isNotEmpty()) {
                    this.modifiers.add(trimmed)
                }
            }
        } else {
            this.modelName = rawModel
        }
    }

    private fun getRawModel(name: String): String {
        return if (name.contains("|")) {
            name.split("|")[0]
        } else {
            name
        }
    }

    private fun getRawScale(name: String): Float {
        return if (name.contains("|")) {
            name.split("|")[1].toFloatOrNull() ?: 1.0f
        } else {
            1.0f
        }
    }

    fun calculateIdealConfiguration(parts: List<ModelPart>) {
        if (parts.isEmpty()) return

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE

        parts.forEach { part ->
            minX = min(minX, part.minimumCoords.x * 16f)
            minY = min(minY, part.minimumCoords.y * 16f)
            minZ = min(minZ, part.minimumCoords.z * 16f)
            maxX = max(maxX, part.maximumCoords.x * 16f)
            maxY = max(maxY, part.maximumCoords.y * 16f)
            maxZ = max(maxZ, part.maximumCoords.z * 16f)
        }

        val width = maxX - minX
        val height = maxY - minY
        val depth = maxZ - minZ

        val padding = 0.1f
        this.collisionBounds = FVec(
            width + padding,
            height + padding,
            depth + padding
        )

        this.pickingBoundsMin = FVec(minX - padding, minY - padding, minZ - padding)
        this.pickingBoundsMax = FVec(maxX + padding, maxY + padding, maxZ + padding)

        this.nameY = maxY + 2.0f
        this.eyeY = maxY - (maxY * 0.15f)
    }

    fun getFullName(): String {
        var name = this.modelName
        if (this.modifiers.isNotEmpty()) {
            val sortedModifiers = this.modifiers.sorted()
            name += "(${sortedModifiers.joinToString(",")})"
        }
        return name
    }

    fun getFullNameWithScale(): String {
        var name = getFullName()
        if (this.scale != 1.0f) {
            name += "|${this.scale}"
        }
        return name
    }

    fun addModifier(modifier: String): Boolean {
        return this.modifiers.add(modifier)
    }

    fun removeModifier(modifier: String): Boolean {
        return this.modifiers.remove(modifier)
    }

    fun isPersonal(): Boolean {
        return modelName.contains("+")
    }

    fun isPersonalPrimary(): Boolean {
        return modelName.endsWith("+")
    }

    fun loadFromModel(model: Model) {
        this.nameY = model.nameY * 16.0f
        this.eyeY = model.eyeY * 16.0f
        this.collisionBounds = FVec(
            model.collisionSize.x * 16.0f,
            model.collisionSize.y * 16.0f,
            model.collisionSize.z * 16.0f
        )
        this.pickingBoundsMin = FVec(
            model.pickingBoundsMin.x * 16.0f,
            model.pickingBoundsMin.y * 16.0f,
            model.pickingBoundsMin.z * 16.0f
        )
        this.pickingBoundsMax = FVec(
            model.pickingBoundsMax.x * 16.0f,
            model.pickingBoundsMax.y * 16.0f,
            model.pickingBoundsMax.z * 16.0f
        )
        this.bobbing = (model.flags.toInt() and Model.FLAG_BOBBING.toInt()) != 0
        this.pushes = (model.flags.toInt() and Model.FLAG_PUSHES.toInt()) != 0
        this.usesHumanSkin = (model.flags.toInt() and Model.FLAG_USES_HUMAN_SKIN.toInt()) != 0
        this.calcHumanAnims = (model.flags.toInt() and Model.FLAG_CALC_HUMAN_ANIMS.toInt()) != 0
    }

    private fun getModelFileName(): String {
        return (this.fileName ?: this.modelName).lowercase()
    }

    private fun getFolderPath(): String {
        return "models/"
    }

    fun getCCPath(): String {
        val modelName = getModelFileName()
        return getFolderPath() + File(modelName).nameWithoutExtension + ".ccmodel"
    }

    fun getBBPath(): String {
        val modelName = getModelFileName()
        return getFolderPath() + File(modelName).nameWithoutExtension + ".bbmodel"
    }

    fun exists(): Boolean {
        return File(getCCPath()).exists()
    }

    fun delete() {
        File(getCCPath()).delete()
        File(getBBPath()).delete()
    }
}

object ModelConfigurationManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun loadConfiguration(name: String): ModelConfiguration {
        val config = ModelConfiguration(name)

        if (config.exists()) {
            try {
                val content = File(config.getCCPath()).readText()
                val loaded = gson.fromJson(content, ModelConfiguration::class.java)

                config.nameY = loaded.nameY
                config.eyeY = loaded.eyeY
                config.collisionBounds = loaded.collisionBounds
                config.pickingBoundsMin = loaded.pickingBoundsMin
                config.pickingBoundsMax = loaded.pickingBoundsMax
                config.bobbing = loaded.bobbing
                config.pushes = loaded.pushes
                config.usesHumanSkin = loaded.usesHumanSkin
                config.calcHumanAnims = loaded.calcHumanAnims
                config.defaultSkin = loaded.defaultSkin
            } catch (e: Exception) {
                Console.errLog("Failed to load model configuration ${config.getFullName()}: ${e.message}")
                saveConfiguration(config)
            }
        } else {
            saveConfiguration(config)
        }

        return config
    }

    fun saveConfiguration(config: ModelConfiguration) {
        try {
            val path = config.getCCPath()
            val directory = File(path).parentFile

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val json = gson.toJson(config)
            File(path).writeText(json)
        } catch (e: Exception) {
            Console.errLog("Failed to save model configuration ${config.getFullName()}: ${e.message}")
        }
    }

    fun ensureConfigurationExists(name: String): ModelConfiguration {
        val config = loadConfiguration(name)
        if (!config.exists()) {
            saveConfiguration(config)
        }
        return config
    }
}
