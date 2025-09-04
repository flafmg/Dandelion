package org.dandelion.server.plugins.manager

import java.io.File
import java.net.URLClassLoader

class PluginClassLoader(
    parent: ClassLoader,
    private val pluginJars: List<File>,
) :
    URLClassLoader(
        pluginJars.map { it.toURI().toURL() }.toTypedArray(),
        parent,
    )
