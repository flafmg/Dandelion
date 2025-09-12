package org.dandelion.server.models

import org.dandelion.server.entity.player.Player
import org.dandelion.server.network.packets.cpe.server.ServerDefineModel
import org.dandelion.server.network.packets.cpe.server.ServerDefineModelPart
import org.dandelion.server.types.AnimData
import org.dandelion.server.types.UVCoords
import org.dandelion.server.types.vec.FVec

data class ModelPart(
    val minimumCoords: FVec,
    val maximumCoords: FVec,
    val topFaceUV: UVCoords,
    val bottomFaceUV: UVCoords,
    val frontFaceUV: UVCoords,
    val backFaceUV: UVCoords,
    val leftFaceUV: UVCoords,
    val rightFaceUV: UVCoords,
    val rotationOrigin: FVec,
    val rotationAngles: FVec,
    val animation1: AnimData = AnimData.create(AnimData.AXIS_X, AnimData.TYPE_NONE),
    val animation2: AnimData = AnimData.create(AnimData.AXIS_X, AnimData.TYPE_NONE),
    val animation3: AnimData = AnimData.create(AnimData.AXIS_X, AnimData.TYPE_NONE),
    val animation4: AnimData = AnimData.create(AnimData.AXIS_X, AnimData.TYPE_NONE),
    val flags: UByte = 0u
) {
    companion object {
        const val FLAG_FULL_BRIGHT: UByte = 0x01u
        const val FLAG_FIRST_PERSON_ARM: UByte = 0x02u
    }

    override fun toString(): String {
        return """ModelPart {
            |  minimumCoords: $minimumCoords
            |  maximumCoords: $maximumCoords
            |  rotationOrigin: $rotationOrigin
            |  rotationAngles: $rotationAngles
            |  topFaceUV: $topFaceUV
            |  bottomFaceUV: $bottomFaceUV
            |  frontFaceUV: $frontFaceUV
            |  backFaceUV: $backFaceUV
            |  leftFaceUV: $leftFaceUV
            |  rightFaceUV: $rightFaceUV
            |  flags: $flags
            |}""".trimMargin()
    }
}

data class Model(
    val id: UByte,
    val name: String,
    val flags: UByte = (FLAG_BOBBING or FLAG_PUSHES or FLAG_USES_HUMAN_SKIN or FLAG_CALC_HUMAN_ANIMS),
    val nameY: Float = 32.5f / 16.0f,
    val eyeY: Float = 26.0f / 16.0f,
    val collisionSize: FVec = FVec(8.6f / 16.0f, 28.1f / 16.0f, 8.6f / 16.0f),
    val pickingBoundsMin: FVec = FVec(-8f / 16.0f, 0f / 16.0f, -4f / 16.0f),
    val pickingBoundsMax: FVec = FVec(8f / 16.0f, 32f / 16.0f, 4f / 16.0f),
    val uScale: UShort = 64u,
    val vScale: UShort = 64u,
    val parts: List<ModelPart> = emptyList()
) {
    companion object {
        const val FLAG_BOBBING: UByte = 0x01u
        const val FLAG_PUSHES: UByte = 0x02u
        const val FLAG_USES_HUMAN_SKIN: UByte = 0x04u
        const val FLAG_CALC_HUMAN_ANIMS: UByte = 0x08u
    }

    val partsCount: UByte get() = parts.size.toUByte()
    
    fun sendToPlayer(player: Player) {
        val defineModelPacket = ServerDefineModel(
            modelId = this.id,
            name = this.name,
            flags = this.flags,
            nameY = this.nameY,
            eyeY = this.eyeY,
            collisionSize = this.collisionSize,
            pickingBoundsMin = this.pickingBoundsMin,
            pickingBoundsMax = this.pickingBoundsMax,
            uScale = this.uScale,
            vScale = this.vScale,
            partsCount = this.partsCount
        )
        defineModelPacket.send(player)

        this.parts.forEach { part ->
            val definePartPacket = ServerDefineModelPart(
                modelId = this.id,
                minimumCoords = part.minimumCoords,
                maximumCoords = part.maximumCoords,
                topFaceUV = part.topFaceUV,
                bottomFaceUV = part.bottomFaceUV,
                frontFaceUV = part.frontFaceUV,
                backFaceUV = part.backFaceUV,
                leftFaceUV = part.leftFaceUV,
                rightFaceUV = part.rightFaceUV,
                rotationOrigin = part.rotationOrigin,
                rotationAngles = part.rotationAngles,
                animation1 = part.animation1,
                animation2 = part.animation2,
                animation3 = part.animation3,
                animation4 = part.animation4,
                flags = part.flags
            )
            definePartPacket.send(player)
        }
    }

    override fun toString(): String {
        val flagsDesc = mutableListOf<String>()
        if ((flags.toInt() and FLAG_BOBBING.toInt()) != 0) flagsDesc.add("BOBBING")
        if ((flags.toInt() and FLAG_PUSHES.toInt()) != 0) flagsDesc.add("PUSHES")
        if ((flags.toInt() and FLAG_USES_HUMAN_SKIN.toInt()) != 0) flagsDesc.add("HUMAN_SKIN")
        if ((flags.toInt() and FLAG_CALC_HUMAN_ANIMS.toInt()) != 0) flagsDesc.add("HUMAN_ANIMS")

        return """Model {
            |  ID: $id
            |  Name: '$name'
            |  Parts: $partsCount
            |  Texture: ${uScale}x$vScale
            |  Collision: $collisionSize
            |  Picking: $pickingBoundsMin to $pickingBoundsMax
            |  Name Height: $nameY, Eye Height: $eyeY
            |  Flags: ${if (flagsDesc.isEmpty()) "None" else flagsDesc.joinToString(", ")}
            |}""".trimMargin()
    }
}