package org.dandelion.classic.types.enums

enum class EntityModel(val string: String) {
    HUMANOID("humanoid"),
    CHICKEN("chicken"),
    CREEPER("creeper"),
    CROCODILE("croc"),
    PIG("pig"),
    PRINTER("printer"),
    SHEEP("sheep"),
    SKELETON("skeleton"),
    SPIDER("spider"),
    ZOMBIE("zombie"),
    HEAD("head"),
    SITTING("sitting"),
    CHIBI("chibi");

    companion object {
        fun fromString(modelString: String): EntityModel {
            return values().find { it.string == modelString } ?: HUMANOID
        }

        fun isValidModel(modelString: String): Boolean {
            return values().any { it.string == modelString }
        }

        fun isBlockModel(modelString: String): Boolean {
            return modelString.toByteOrNull() != null
        }
    }
}
