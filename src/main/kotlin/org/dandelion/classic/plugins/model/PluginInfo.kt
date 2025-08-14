package org.dandelion.classic.plugins.model

data class PluginInfo(
    val name: String,
    val version: String,
    val description: String,
    val authors: List<String>,
    val dependencies: List<Pair<String, String?>>,
) {
    companion object {
        fun empty(): PluginInfo =
            PluginInfo(
                name = "",
                version = "",
                description = "",
                authors = emptyList(),
                dependencies = emptyList(),
            )
    }
}
