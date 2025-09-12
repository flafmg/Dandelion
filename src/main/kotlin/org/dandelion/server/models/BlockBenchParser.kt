package org.dandelion.server.models

import com.google.gson.JsonObject
import org.dandelion.server.types.AnimData
import org.dandelion.server.types.UVCoords
import org.dandelion.server.types.vec.FVec
import org.dandelion.server.util.JsonConfig
import java.io.File

object BlockBenchParser {
    private const val MAX_PARTS = 64

    fun parseModelFromFile(bbmodelFile: File): Model? {
        val modelName = bbmodelFile.nameWithoutExtension
        val modelId = ModelRegistry.getNextAvailableId() ?: return null
        val parts = parseModelParts(bbmodelFile) ?: return null
        if (parts.isEmpty()) return null

        val config = JsonConfig.Companion.load(bbmodelFile)
        val textureWidth = config.getInt("resolution.width") ?: 64
        val textureHeight = config.getInt("resolution.height") ?: 64
        val (collisionSize, pickingBoundsMin, pickingBoundsMax) = calculateModelBounds(parts)

        return Model(
            id = modelId,
            name = modelName,
            flags = 0u,
            nameY = 1.2f,
            eyeY = 0.7f,
            collisionSize = collisionSize,
            pickingBoundsMin = pickingBoundsMin,
            pickingBoundsMax = pickingBoundsMax,
            uScale = textureWidth.toUShort(),
            vScale = textureHeight.toUShort(),
            parts = parts
        )
    }

    fun parseModelParts(bbmodelFile: File): List<ModelPart>? {
        return try {
            if (!bbmodelFile.exists()) return null
            val config = JsonConfig.Companion.load(bbmodelFile)
            parsePartsFromConfig(config)
        } catch (e: Exception) {
            null
        }
    }

    fun parsePartsFromConfig(config: JsonConfig): List<ModelPart> {
        val elements = config.getArray("elements") ?: return emptyList()
        val outliner = config.getArray("outliner") ?: return emptyList()

        val elementByUuid = mutableMapOf<String, JsonObject>()
        for (i in 0 until elements.size()) {
            val element = elements[i].asJsonObject
            val uuid = element.get("uuid")?.asString
            if (uuid != null) {
                elementByUuid[uuid] = element
            }
        }

        val parts = mutableListOf<ModelPart>()

        for (i in 0 until outliner.size()) {
            val outlinerItem = outliner[i]
            if (outlinerItem.isJsonPrimitive && outlinerItem.asJsonPrimitive.isString) {
                val uuid = outlinerItem.asString
                val element = elementByUuid[uuid]
                if (element != null) {
                    val part = parseElement(element)
                    if (part != null && parts.size < MAX_PARTS) {
                        parts.add(part)
                    }
                }
            } else if (outlinerItem.isJsonObject) {
                handleGroup(outlinerItem.asJsonObject, elementByUuid, parts, floatArrayOf(0f, 0f, 0f), true)
            }
        }

        return if (parts.size > MAX_PARTS) parts.take(MAX_PARTS) else parts
    }

    private fun handleGroup(
        group: JsonObject,
        elementByUuid: Map<String, JsonObject>,
        parts: MutableList<ModelPart>,
        rotation: FloatArray,
        visibility: Boolean
    ) {
        val children = group.getAsJsonArray("children") ?: return
        val groupVisibility = group.get("visibility")?.asBoolean ?: true
        val finalVisibility = visibility && groupVisibility

        val groupRotation = group.getAsJsonArray("rotation")
        val totalRotation = if (groupRotation != null && groupRotation.size() == 3) {
            floatArrayOf(
                rotation[0] + groupRotation[0].asFloat,
                rotation[1] + groupRotation[1].asFloat,
                rotation[2] + groupRotation[2].asFloat
            )
        } else {
            rotation
        }

        for (i in 0 until children.size()) {
            val child = children[i]
            if (child.isJsonPrimitive && child.asJsonPrimitive.isString) {
                val uuid = child.asString
                val element = elementByUuid[uuid]
                if (element != null && parts.size < MAX_PARTS) {
                    val modifiedElement = element.deepCopy()

                    val elementRotation = modifiedElement.getAsJsonArray("rotation")
                    if (elementRotation != null && elementRotation.size() == 3) {
                        elementRotation[0] = com.google.gson.JsonPrimitive(elementRotation[0].asFloat + totalRotation[0])
                        elementRotation[1] = com.google.gson.JsonPrimitive(elementRotation[1].asFloat + totalRotation[1])
                        elementRotation[2] = com.google.gson.JsonPrimitive(elementRotation[2].asFloat + totalRotation[2])
                    }

                    modifiedElement.addProperty("visibility", finalVisibility)

                    val part = parseElement(modifiedElement)
                    if (part != null) {
                        parts.add(part)
                    }
                }
            } else if (child.isJsonObject) {
                handleGroup(child.asJsonObject, elementByUuid, parts, totalRotation, finalVisibility)
            }
        }
    }

    private fun parseElement(element: JsonObject): ModelPart? {
        return try {
            val visibility = element.get("visibility")?.asBoolean ?: true
            if (!visibility) return null

            val fromArray = element.getAsJsonArray("from")
            val toArray = element.getAsJsonArray("to")

            if (fromArray == null || toArray == null || fromArray.size() != 3 || toArray.size() != 3) {
                return null
            }

            val inflate = element.get("inflate")?.asFloat ?: 0f

            val minimumCoords = FVec(
                (fromArray[0].asFloat - inflate) / 16f,
                (fromArray[1].asFloat - inflate) / 16f,
                (fromArray[2].asFloat - inflate) / 16f
            )

            val maximumCoords = FVec(
                (toArray[0].asFloat + inflate) / 16f,
                (toArray[1].asFloat + inflate) / 16f,
                (toArray[2].asFloat + inflate) / 16f
            )

            val originArray = element.getAsJsonArray("origin")
            val rotationOrigin = if (originArray != null && originArray.size() == 3) {
                FVec(
                    originArray[0].asFloat / 16f,
                    originArray[1].asFloat / 16f,
                    originArray[2].asFloat / 16f
                )
            } else {
                FVec(
                    (minimumCoords.x + maximumCoords.x) / 2f,
                    (minimumCoords.y + maximumCoords.y) / 2f,
                    (minimumCoords.z + maximumCoords.z) / 2f
                )
            }

            val rotationArray = element.getAsJsonArray("rotation")
            val rotationAngles = if (rotationArray != null && rotationArray.size() == 3) {
                FVec(
                    rotationArray[0].asFloat,
                    rotationArray[1].asFloat,
                    rotationArray[2].asFloat
                )
            } else {
                FVec(0f, 0f, 0f)
            }

            val facesObject = element.getAsJsonObject("faces")
            val uvCoords = parseFaceUVs(facesObject)

            ModelPart(
                minimumCoords = minimumCoords,
                maximumCoords = maximumCoords,
                topFaceUV = uvCoords["up"] ?: getDefaultUV(),
                bottomFaceUV = uvCoords["down"] ?: getDefaultUV(),
                frontFaceUV = uvCoords["north"] ?: getDefaultUV(),
                backFaceUV = uvCoords["south"] ?: getDefaultUV(),
                leftFaceUV = uvCoords["west"] ?: getDefaultUV(),
                rightFaceUV = uvCoords["east"] ?: getDefaultUV(),
                rotationOrigin = rotationOrigin,
                rotationAngles = rotationAngles,
                animation1 = AnimData.create(AnimData.AXIS_X, AnimData.TYPE_NONE),
                animation2 = AnimData.create(AnimData.AXIS_X, AnimData.TYPE_NONE),
                animation3 = AnimData.create(AnimData.AXIS_X, AnimData.TYPE_NONE),
                animation4 = AnimData.create(AnimData.AXIS_X, AnimData.TYPE_NONE),
                flags = 0u
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseFaceUVs(facesObject: JsonObject?): Map<String, UVCoords> {
        val uvMap = mutableMapOf<String, UVCoords>()
        if (facesObject == null) return uvMap

        fun parseFaceUV(faceName: String, faceObject: JsonObject?): UVCoords? {
            if (faceObject == null) return null
            val uvArray = faceObject.getAsJsonArray("uv")
            if (uvArray == null || uvArray.size() != 4) return null

            return when (faceName) {
                "up" -> UVCoords(
                    u1 = uvArray[2].asFloat.toInt().toUShort(),
                    v1 = uvArray[3].asFloat.toInt().toUShort(),
                    u2 = uvArray[0].asFloat.toInt().toUShort(),
                    v2 = uvArray[1].asFloat.toInt().toUShort()
                )
                else -> UVCoords(
                    u1 = uvArray[0].asFloat.toInt().toUShort(),
                    v1 = uvArray[1].asFloat.toInt().toUShort(),
                    u2 = uvArray[2].asFloat.toInt().toUShort(),
                    v2 = uvArray[3].asFloat.toInt().toUShort()
                )
            }
        }

        val faceNames = listOf("up", "down", "north", "south", "east", "west")
        for (faceName in faceNames) {
            val faceObject = facesObject.getAsJsonObject(faceName)
            val uvCoords = parseFaceUV(faceName, faceObject)
            if (uvCoords != null) {
                uvMap[faceName] = uvCoords
            }
        }

        return uvMap
    }

    private fun getDefaultUV(): UVCoords {
        return UVCoords(0u, 0u, 16u, 16u)
    }

    private fun calculateModelBounds(parts: List<ModelPart>): Triple<FVec, FVec, FVec> {
        if (parts.isEmpty()) {
            return Triple(
                FVec(0.6f, 1.8f, 0.6f),
                FVec(-0.3f, 0f, -0.3f),
                FVec(0.3f, 1.8f, 0.3f)
            )
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE

        parts.forEach { part ->
            minX = kotlin.math.min(minX, part.minimumCoords.x)
            minY = kotlin.math.min(minY, part.minimumCoords.y)
            minZ = kotlin.math.min(minZ, part.minimumCoords.z)
            maxX = kotlin.math.max(maxX, part.maximumCoords.x)
            maxY = kotlin.math.max(maxY, part.maximumCoords.y)
            maxZ = kotlin.math.max(maxZ, part.maximumCoords.z)
        }

        val width = maxX - minX
        val height = maxY - minY
        val depth = maxZ - minZ

        val collisionSize = FVec(width, height, depth)
        val pickingBoundsMin = FVec(minX, minY, minZ)
        val pickingBoundsMax = FVec(maxX, maxY, maxZ)

        return Triple(collisionSize, pickingBoundsMin, pickingBoundsMax)
    }
}
