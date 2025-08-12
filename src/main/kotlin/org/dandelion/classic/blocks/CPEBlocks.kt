package org.dandelion.classic.blocks

import org.dandelion.classic.blocks.model.Block

class CobblestoneSlab : Block() {
    override val id: Byte = 50
    override val name: String = "Cobblestone Slab"
    override val fallback: Byte = 44
    override val isDefault: Boolean = true
}

class Rope : Block() {
    override val id: Byte = 51
    override val name: String = "Rope"
    override val fallback: Byte = 39
    override val isDefault: Boolean = true
}

class Sandstone : Block() {
    override val id: Byte = 52
    override val name: String = "Sandstone"
    override val fallback: Byte = 12
    override val isDefault: Boolean = true
}

class Snow : Block() {
    override val id: Byte = 53
    override val name: String = "Snow"
    override val fallback: Byte = 0
    override val isDefault: Boolean = true
}

class Fire : Block() {
    override val id: Byte = 54
    override val name: String = "Fire"
    override val fallback: Byte = 10
    override val isDefault: Boolean = true
}

class LightPinkWool : Block() {
    override val id: Byte = 55
    override val name: String = "Light Pink Wool"
    override val fallback: Byte = 33
    override val isDefault: Boolean = true
}

class ForestGreenWool : Block() {
    override val id: Byte = 56
    override val name: String = "Forest Green Wool"
    override val fallback: Byte = 25
    override val isDefault: Boolean = true
}

class BrownWool : Block() {
    override val id: Byte = 57
    override val name: String = "Brown Wool"
    override val fallback: Byte = 3
    override val isDefault: Boolean = true
}

class DeepBlue : Block() {
    override val id: Byte = 58
    override val name: String = "Deep Blue"
    override val fallback: Byte = 29
    override val isDefault: Boolean = true
}

class Turquoise : Block() {
    override val id: Byte = 59
    override val name: String = "Turquoise"
    override val fallback: Byte = 28
    override val isDefault: Boolean = true
}

class Ice : Block() {
    override val id: Byte = 60
    override val name: String = "Ice"
    override val fallback: Byte = 20
    override val isDefault: Boolean = true
}

class CeramicTile : Block() {
    override val id: Byte = 61
    override val name: String = "Ceramic Tile"
    override val fallback: Byte = 42
    override val isDefault: Boolean = true
}

class Magma : Block() {
    override val id: Byte = 62
    override val name: String = "Magma"
    override val fallback: Byte = 49
    override val isDefault: Boolean = true
}

class Pillar : Block() {
    override val id: Byte = 63
    override val name: String = "Pillar"
    override val fallback: Byte = 36
    override val isDefault: Boolean = true
}

class Crate : Block() {
    override val id: Byte = 64
    override val name: String = "Crate"
    override val fallback: Byte = 5
    override val isDefault: Boolean = true
}

class StoneBrick : Block() {
    override val id: Byte = 65
    override val name: String = "Stone Brick"
    override val fallback: Byte = 1
    override val isDefault: Boolean = true
}
