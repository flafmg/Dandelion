package org.dandelion.classic.plugins.model

abstract class Plugin {
    open val info: PluginInfo = PluginInfo.empty()
    abstract fun init()
    abstract fun shutdown()
}