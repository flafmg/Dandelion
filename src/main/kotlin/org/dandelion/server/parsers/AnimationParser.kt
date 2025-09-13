package org.dandelion.server.parsers

import org.dandelion.server.types.AnimData
import kotlin.math.PI

object AnimationParser {

    private const val MATH_PI = PI.toFloat()
    private const val MATH_DEG2RAD = MATH_PI / 180.0f
    private const val ANIM_IDLE_MAX = 3.0f * MATH_DEG2RAD
    private const val ANIM_IDLE_XPERIOD = 2.0f * MATH_PI / 5.0f
    private const val ANIM_IDLE_ZPERIOD = 2.0f * MATH_PI / 3.5f

    data class ParseResult(
        val animations: List<AnimData>,
        val isFullbright: Boolean = false,
        val isHand: Boolean = false,
        val isLayer: Boolean = false,
        val isHumanLeftArm: Boolean = false,
        val isHumanRightArm: Boolean = false,
        val isHumanLeftLeg: Boolean = false,
        val isHumanRightLeg: Boolean = false
    )

    private val animationMappings = mapOf(
        "head" to AnimMapping(AnimData.TYPE_HEAD, AnimData.AXIS_X, 1.0f),
        "headx" to AnimMapping(AnimData.TYPE_HEAD, AnimData.AXIS_X, 1.0f),
        "heady" to AnimMapping(AnimData.TYPE_HEAD, AnimData.AXIS_Y, 1.0f),
        "headz" to AnimMapping(AnimData.TYPE_HEAD, AnimData.AXIS_Z, 1.0f),

        "leftleg" to AnimMapping(AnimData.TYPE_LEFT_LEG_X, AnimData.AXIS_X, 1.0f),
        "leftlegx" to AnimMapping(AnimData.TYPE_LEFT_LEG_X, AnimData.AXIS_X, 1.0f),
        "leftlegy" to AnimMapping(AnimData.TYPE_LEFT_LEG_X, AnimData.AXIS_Y, 1.0f),
        "leftlegz" to AnimMapping(AnimData.TYPE_LEFT_LEG_X, AnimData.AXIS_Z, 1.0f),

        "rightleg" to AnimMapping(AnimData.TYPE_RIGHT_LEG_X, AnimData.AXIS_X, 1.0f),
        "rightlegx" to AnimMapping(AnimData.TYPE_RIGHT_LEG_X, AnimData.AXIS_X, 1.0f),
        "rightlegy" to AnimMapping(AnimData.TYPE_RIGHT_LEG_X, AnimData.AXIS_Y, 1.0f),
        "rightlegz" to AnimMapping(AnimData.TYPE_RIGHT_LEG_X, AnimData.AXIS_Z, 1.0f),

        "leftarm" to AnimMapping(
            listOf(AnimData.TYPE_LEFT_ARM_X, AnimData.TYPE_LEFT_ARM_Z),
            listOf(AnimData.AXIS_X, AnimData.AXIS_Z),
            1.0f
        ),
        "leftarmxx" to AnimMapping(AnimData.TYPE_LEFT_ARM_X, AnimData.AXIS_X, 1.0f),
        "leftarmxy" to AnimMapping(AnimData.TYPE_LEFT_ARM_X, AnimData.AXIS_Y, 1.0f),
        "leftarmxz" to AnimMapping(AnimData.TYPE_LEFT_ARM_X, AnimData.AXIS_Z, 1.0f),

        "rightarm" to AnimMapping(
            listOf(AnimData.TYPE_RIGHT_ARM_X, AnimData.TYPE_RIGHT_ARM_Z),
            listOf(AnimData.AXIS_X, AnimData.AXIS_Z),
            1.0f
        ),
        "rightarmxx" to AnimMapping(AnimData.TYPE_RIGHT_ARM_X, AnimData.AXIS_X, 1.0f),
        "rightarmxy" to AnimMapping(AnimData.TYPE_RIGHT_ARM_X, AnimData.AXIS_Y, 1.0f),
        "rightarmxz" to AnimMapping(AnimData.TYPE_RIGHT_ARM_X, AnimData.AXIS_Z, 1.0f),

        "leftarmzx" to AnimMapping(AnimData.TYPE_LEFT_ARM_Z, AnimData.AXIS_X, 1.0f),
        "leftarmzy" to AnimMapping(AnimData.TYPE_LEFT_ARM_Z, AnimData.AXIS_Y, 1.0f),
        "leftarmzz" to AnimMapping(AnimData.TYPE_LEFT_ARM_Z, AnimData.AXIS_Z, 1.0f),

        "rightarmzx" to AnimMapping(AnimData.TYPE_RIGHT_ARM_Z, AnimData.AXIS_X, 1.0f),
        "rightarmzy" to AnimMapping(AnimData.TYPE_RIGHT_ARM_Z, AnimData.AXIS_Y, 1.0f),
        "rightarmzz" to AnimMapping(AnimData.TYPE_RIGHT_ARM_Z, AnimData.AXIS_Z, 1.0f),

        "spinx" to AnimMapping(AnimData.TYPE_SPIN, AnimData.AXIS_X, 1.0f, 0.0f),
        "spiny" to AnimMapping(AnimData.TYPE_SPIN, AnimData.AXIS_Y, 1.0f, 0.0f),
        "spinz" to AnimMapping(AnimData.TYPE_SPIN, AnimData.AXIS_Z, 1.0f, 0.0f),

        "spinxvelocity" to AnimMapping(AnimData.TYPE_SPIN_VELOCITY, AnimData.AXIS_X, 1.0f, 0.0f),
        "spinyvelocity" to AnimMapping(AnimData.TYPE_SPIN_VELOCITY, AnimData.AXIS_Y, 1.0f, 0.0f),
        "spinzvelocity" to AnimMapping(AnimData.TYPE_SPIN_VELOCITY, AnimData.AXIS_Z, 1.0f, 0.0f),

        "sinx" to AnimMapping(AnimData.TYPE_SIN_ROTATE, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 0.0f),
        "siny" to AnimMapping(AnimData.TYPE_SIN_ROTATE, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 0.0f),
        "sinz" to AnimMapping(AnimData.TYPE_SIN_ROTATE, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 0.0f),

        "sinxvelocity" to AnimMapping(AnimData.TYPE_SIN_ROTATE_VELOCITY, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 0.0f),
        "sinyvelocity" to AnimMapping(AnimData.TYPE_SIN_ROTATE_VELOCITY, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 0.0f),
        "sinzvelocity" to AnimMapping(AnimData.TYPE_SIN_ROTATE_VELOCITY, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 0.0f),

        "cosx" to AnimMapping(AnimData.TYPE_SIN_ROTATE, AnimData.AXIS_X, 1.0f, 1.0f, 0.25f, 0.0f),
        "cosy" to AnimMapping(AnimData.TYPE_SIN_ROTATE, AnimData.AXIS_Y, 1.0f, 1.0f, 0.25f, 0.0f),
        "cosz" to AnimMapping(AnimData.TYPE_SIN_ROTATE, AnimData.AXIS_Z, 1.0f, 1.0f, 0.25f, 0.0f),

        "cosxvelocity" to AnimMapping(AnimData.TYPE_SIN_ROTATE_VELOCITY, AnimData.AXIS_X, 1.0f, 1.0f, 0.25f, 0.0f),
        "cosyvelocity" to AnimMapping(AnimData.TYPE_SIN_ROTATE_VELOCITY, AnimData.AXIS_Y, 1.0f, 1.0f, 0.25f, 0.0f),
        "coszvelocity" to AnimMapping(AnimData.TYPE_SIN_ROTATE_VELOCITY, AnimData.AXIS_Z, 1.0f, 1.0f, 0.25f, 0.0f),

        "pistonx" to AnimMapping(AnimData.TYPE_SIN_TRANSLATE, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 0.0f),
        "pistony" to AnimMapping(AnimData.TYPE_SIN_TRANSLATE, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 0.0f),
        "pistonz" to AnimMapping(AnimData.TYPE_SIN_TRANSLATE, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 0.0f),

        "pistonxvelocity" to AnimMapping(AnimData.TYPE_SIN_TRANSLATE_VELOCITY, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 0.0f),
        "pistonyvelocity" to AnimMapping(AnimData.TYPE_SIN_TRANSLATE_VELOCITY, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 0.0f),
        "pistonzvelocity" to AnimMapping(AnimData.TYPE_SIN_TRANSLATE_VELOCITY, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 0.0f),

        "pulsate" to AnimMapping(
            listOf(AnimData.TYPE_SIN_SIZE, AnimData.TYPE_SIN_SIZE, AnimData.TYPE_SIN_SIZE),
            listOf(AnimData.AXIS_X, AnimData.AXIS_Y, AnimData.AXIS_Z),
            1.0f, 1.0f, 0.0f, 0.0f
        ),
        "pulsatex" to AnimMapping(AnimData.TYPE_SIN_SIZE, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 0.0f),
        "pulsatey" to AnimMapping(AnimData.TYPE_SIN_SIZE, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 0.0f),
        "pulsatez" to AnimMapping(AnimData.TYPE_SIN_SIZE, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 0.0f),

        "pulsatevelocity" to AnimMapping(
            listOf(AnimData.TYPE_SIN_SIZE_VELOCITY, AnimData.TYPE_SIN_SIZE_VELOCITY, AnimData.TYPE_SIN_SIZE_VELOCITY),
            listOf(AnimData.AXIS_X, AnimData.AXIS_Y, AnimData.AXIS_Z),
            1.0f, 1.0f, 0.0f, 0.0f
        ),
        "pulsatexvelocity" to AnimMapping(AnimData.TYPE_SIN_SIZE_VELOCITY, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 0.0f),
        "pulsateyvelocity" to AnimMapping(AnimData.TYPE_SIN_SIZE_VELOCITY, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 0.0f),
        "pulsatezvelocity" to AnimMapping(AnimData.TYPE_SIN_SIZE_VELOCITY, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 0.0f),

        "flipx" to AnimMapping(AnimData.TYPE_FLIP_ROTATE, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 1.0f),
        "flipy" to AnimMapping(AnimData.TYPE_FLIP_ROTATE, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 1.0f),
        "flipz" to AnimMapping(AnimData.TYPE_FLIP_ROTATE, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 1.0f),

        "flipxvelocity" to AnimMapping(AnimData.TYPE_FLIP_ROTATE_VELOCITY, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 1.0f),
        "flipyvelocity" to AnimMapping(AnimData.TYPE_FLIP_ROTATE_VELOCITY, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 1.0f),
        "flipzvelocity" to AnimMapping(AnimData.TYPE_FLIP_ROTATE_VELOCITY, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 1.0f),

        "fliptranslatex" to AnimMapping(AnimData.TYPE_FLIP_TRANSLATE, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 1.0f),
        "fliptranslatey" to AnimMapping(AnimData.TYPE_FLIP_TRANSLATE, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 1.0f),
        "fliptranslatez" to AnimMapping(AnimData.TYPE_FLIP_TRANSLATE, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 1.0f),

        "fliptranslatexvelocity" to AnimMapping(AnimData.TYPE_FLIP_TRANSLATE_VELOCITY, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 1.0f),
        "fliptranslateyvelocity" to AnimMapping(AnimData.TYPE_FLIP_TRANSLATE_VELOCITY, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 1.0f),
        "fliptranslatezvelocity" to AnimMapping(AnimData.TYPE_FLIP_TRANSLATE_VELOCITY, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 1.0f),

        "flipsizex" to AnimMapping(AnimData.TYPE_FLIP_SIZE, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 1.0f),
        "flipsizey" to AnimMapping(AnimData.TYPE_FLIP_SIZE, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 1.0f),
        "flipsizez" to AnimMapping(AnimData.TYPE_FLIP_SIZE, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 1.0f),

        "flipsizexvelocity" to AnimMapping(AnimData.TYPE_FLIP_SIZE_VELOCITY, AnimData.AXIS_X, 1.0f, 1.0f, 0.0f, 1.0f),
        "flipsizeyvelocity" to AnimMapping(AnimData.TYPE_FLIP_SIZE_VELOCITY, AnimData.AXIS_Y, 1.0f, 1.0f, 0.0f, 1.0f),
        "flipsizezvelocity" to AnimMapping(AnimData.TYPE_FLIP_SIZE_VELOCITY, AnimData.AXIS_Z, 1.0f, 1.0f, 0.0f, 1.0f)
    )

    fun parseAnimations(elementName: String): ParseResult {
        val animations = mutableListOf<AnimData>()
        var isFullbright = false
        var isHand = false
        var isLayer = false
        var isHumanLeftArm = false
        var isHumanRightArm = false
        var isHumanLeftLeg = false
        var isHumanRightLeg = false

        val cleanName = elementName.replace(" ", "")
        val attributes = cleanName.split(",")

        for (attribute in attributes) {
            if (attribute.trim().isEmpty()) continue

            val colonSplit = attribute.split(":")
            val attrName = colonSplit[0].lowercase()

            val params = if (colonSplit.size >= 2) {
                val modifiers = colonSplit[1].replace(" ", "").split("|")
                FloatArray(4) { i ->
                    if (i < modifiers.size) {
                        modifiers[i].toFloatOrNull() ?: 0f
                    } else 0f
                }
            } else {
                FloatArray(4) { 0f }
            }

            when (attrName) {
                "leftidle" -> {
                    animations.add(AnimData.create(
                        AnimData.AXIS_X, AnimData.TYPE_SIN_ROTATE,
                        ANIM_IDLE_XPERIOD, ANIM_IDLE_MAX, 0f, 0f
                    ))
                    animations.add(AnimData.create(
                        AnimData.AXIS_Z, AnimData.TYPE_SIN_ROTATE,
                        ANIM_IDLE_ZPERIOD, -ANIM_IDLE_MAX, 0.25f, 1f
                    ))
                }
                "rightidle" -> {
                    animations.add(AnimData.create(
                        AnimData.AXIS_X, AnimData.TYPE_SIN_ROTATE,
                        ANIM_IDLE_XPERIOD, -ANIM_IDLE_MAX, 0f, 0f
                    ))
                    animations.add(AnimData.create(
                        AnimData.AXIS_Z, AnimData.TYPE_SIN_ROTATE,
                        ANIM_IDLE_ZPERIOD, ANIM_IDLE_MAX, 0.25f, 1f
                    ))
                }
                "fullbright" -> isFullbright = true
                "hand" -> isHand = true
                "layer" -> isLayer = true
                "humanleftarm" -> isHumanLeftArm = true
                "humanrightarm" -> isHumanRightArm = true
                "humanleftleg" -> isHumanLeftLeg = true
                "humanrightleg" -> isHumanRightLeg = true
                else -> {
                    animationMappings[attrName]?.let { mapping ->
                        animations.addAll(mapping.toAnimData(params))
                    }
                }
            }
        }

        return ParseResult(
            animations = animations,
            isFullbright = isFullbright,
            isHand = isHand,
            isLayer = isLayer,
            isHumanLeftArm = isHumanLeftArm,
            isHumanRightArm = isHumanRightArm,
            isHumanLeftLeg = isHumanLeftLeg,
            isHumanRightLeg = isHumanRightLeg
        )
    }

    private data class AnimMapping(
        val types: List<Int>,
        val axes: List<Int>,
        val defaultA: Float,
        val defaultB: Float = 1.0f,
        val defaultC: Float = 1.0f,
        val defaultD: Float = 1.0f
    ) {
        constructor(
            type: Int, axis: Int,
            defaultA: Float, defaultB: Float = 1.0f,
            defaultC: Float = 1.0f, defaultD: Float = 1.0f
        ) : this(listOf(type), listOf(axis), defaultA, defaultB, defaultC, defaultD)

        fun toAnimData(params: FloatArray): List<AnimData> {
            val result = mutableListOf<AnimData>()

            for (i in types.indices) {
                val type = types[i]
                val axis = axes[i]

                val a = if (params[0] != 0f) params[0] else defaultA
                val b = if (params[1] != 0f) params[1] else defaultB
                val c = if (params[2] != 0f) params[2] else defaultC
                val d = if (params[3] != 0f) params[3] else defaultD

                if (type in listOf(AnimData.TYPE_FLIP_ROTATE, AnimData.TYPE_FLIP_ROTATE_VELOCITY,
                        AnimData.TYPE_FLIP_TRANSLATE, AnimData.TYPE_FLIP_TRANSLATE_VELOCITY,
                        AnimData.TYPE_FLIP_SIZE, AnimData.TYPE_FLIP_SIZE_VELOCITY)) {
                    if (d == 0f) {
                        throw IllegalArgumentException("Flip animations require non-zero max-value (d parameter)")
                    }
                }

                result.add(AnimData.create(axis, type, a, b, c, d))
            }

            return result
        }
    }
}
