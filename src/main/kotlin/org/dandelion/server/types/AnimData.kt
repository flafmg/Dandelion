package org.dandelion.server.types

data class AnimData(
    val flags: Byte,
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float
) {
    companion object {
        const val AXIS_X = 0
        const val AXIS_Y = 1
        const val AXIS_Z = 2

        const val TYPE_NONE = 0
        const val TYPE_HEAD = 1
        const val TYPE_LEFT_LEG_X = 2
        const val TYPE_RIGHT_LEG_X = 3
        const val TYPE_LEFT_ARM_X = 4
        const val TYPE_LEFT_ARM_Z = 5
        const val TYPE_RIGHT_ARM_X = 6
        const val TYPE_RIGHT_ARM_Z = 7
        const val TYPE_SPIN = 8
        const val TYPE_SPIN_VELOCITY = 9
        const val TYPE_SIN_ROTATE = 10
        const val TYPE_SIN_ROTATE_VELOCITY = 11
        const val TYPE_SIN_TRANSLATE = 12
        const val TYPE_SIN_TRANSLATE_VELOCITY = 13
        const val TYPE_SIN_SIZE = 14
        const val TYPE_SIN_SIZE_VELOCITY = 15
        const val TYPE_FLIP_ROTATE = 16
        const val TYPE_FLIP_ROTATE_VELOCITY = 17
        const val TYPE_FLIP_TRANSLATE = 18
        const val TYPE_FLIP_TRANSLATE_VELOCITY = 19
        const val TYPE_FLIP_SIZE = 20
        const val TYPE_FLIP_SIZE_VELOCITY = 21

        fun create(axis: Int, type: Int, a: Float = 0f, b: Float = 0f, c: Float = 0f, d: Float = 0f): AnimData {
            val flags = ((axis and 0x3) shl 6) or (type and 0x3F)
            return AnimData(flags.toByte(), a, b, c, d)
        }
    }
}
