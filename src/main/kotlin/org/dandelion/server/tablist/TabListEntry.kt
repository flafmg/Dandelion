package org.dandelion.server.tablist

data class TabListEntry(
    val nameId: Short,
    val playerName: String,
    val listName: String,
    val groupName: String,
    val groupRank: Int,
)
