package org.dandelion.classic.plugins.manager

import org.dandelion.classic.plugins.model.Plugin
import org.dandelion.classic.plugins.model.PluginInfo
import org.dandelion.classic.server.Console
import org.dandelion.classic.util.YamlConfig
import java.io.File
import java.util.jar.JarFile

data class LoadedPlugin(
    val info: PluginInfo,
    val instance: Plugin,
    val classLoader: PluginClassLoader,
    val jarFile: File,
)

private data class PluginCluster(
    val plugins: Set<String>,
    val classLoader: PluginClassLoader,
    val jarFiles: List<File>
)

private data class DiscoveredPlugin(
    val info: PluginInfo,
    val jarFile: File
)

internal object PluginRegistry {
    private val loadedPlugins = mutableMapOf<String, LoadedPlugin>()
    private val pluginClusters = mutableListOf<PluginCluster>()
    internal const val PLUGINS_DIR = "plugins"
    internal const val INFO_PATH = "plugin.yml"

    fun init() {
        val pluginsDir = File(PLUGINS_DIR)
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs()
            Console.log("'plugins' folder created in: ${pluginsDir.absolutePath}")
            return
        }
        val discoveredPlugins = discoverPlugins(pluginsDir)
        if (discoveredPlugins.isEmpty()) {
            Console.log("No plugins were found in: ${pluginsDir.absolutePath}")
            return
        }
        val groups = resolveDependencyClusters(discoveredPlugins)
        for (group in groups) {
            try {
                loadPluginCluster(group, discoveredPlugins)
            } catch (e: Exception) {
                Console.log("Error loading group cluster ${e.message}")
                e.printStackTrace()
            }
        }
        initializeAllPlugins()
    }

    fun shutdown() {
        shutdownAllPlugins()
        clearResources()
    }

    fun clearResources() {
        loadedPlugins.clear()
        pluginClusters.forEach { cluster ->
            try {
                cluster.classLoader.close()
            } catch (e: Exception) {
                Console.errLog("Error disabling classloader: ${e.message}")
            }
        }
        pluginClusters.clear()
    }

    private fun discoverPlugins(pluginsDir: File): List<DiscoveredPlugin> {
        val discovered = mutableListOf<DiscoveredPlugin>()
        pluginsDir.listFiles { file -> file.extension == "jar" }?.forEach { jarFile ->
            try {
                val pluginInfo = readInfo(jarFile)
                discovered.add(DiscoveredPlugin(pluginInfo, jarFile))
                Console.log("Plugin found: '${pluginInfo.name}' version: '${pluginInfo.version}'")
            } catch (e: Exception) {
                Console.errLog("Error loading plugin '${jarFile.name}': ${e.message}")
            }
        }
        return discovered
    }

    private fun readInfo(jarFile: File): PluginInfo {
        JarFile(jarFile).use { jar ->
            val entry = jar.getJarEntry(INFO_PATH)
                ?: throw IllegalArgumentException("plugin.yml not found for ${jarFile.name}")
            val config = YamlConfig.load(jar.getInputStream(entry))
            val name = config.getString("name")
                ?: throw java.lang.IllegalArgumentException("Field 'name' not found for ${jarFile.name}")
            val version = config.getString("version", "1.0.0")
            val description = config.getString("description", "none")
            val authors = when {
                config.getStringList("authors") != null -> config.getStringList("authors")!!
                config.getString("author") != null -> listOf(config.getString("author")!!)
                else -> emptyList()
            }
            val dependencies = parseDependencies(config)
            return PluginInfo(name, version, description, authors, dependencies)
        }
    }

    private fun parseDependencies(config: YamlConfig): List<Pair<String, String?>> {
        val deps = mutableListOf<Pair<String, String?>>()
        var index = 0
        while (true) {
            val depSection = config.getSection("dependencies.$index")
            if (depSection == null) break
            val depName = depSection.getString("name")
            if (depName != null) {
                val depVersion = depSection.getString("version")
                deps.add(depName to depVersion)
            }
            index++
        }
        return deps
    }

    private fun resolveDependencyClusters(plugins: List<DiscoveredPlugin>): List<Set<String>> {
        val pluginMap = plugins.associateBy { it.info.name }
        val visited = mutableSetOf<String>()
        val groups = mutableListOf<Set<String>>()
        validateDependencies(plugins, pluginMap)
        detectCircularDependencies(plugins, pluginMap)
        plugins.forEach { plugin ->
            if (plugin.info.name !in visited) {
                val group = findCluster(plugin.info.name, pluginMap, mutableSetOf())
                groups.add(group)
                visited.addAll(group)
            }
        }
        Console.log("Dependency clusters created: ")
        groups.forEachIndexed { index, group ->
            Console.log("  Group: $index: ${group.joinToString(", ")}")
        }
        return groups
    }

    private fun validateDependencies(plugins: List<DiscoveredPlugin>, pluginMap: Map<String, DiscoveredPlugin>) {
        plugins.forEach { plugin ->
            plugin.info.dependencies.forEach { (depName, depVersion) ->
                if (depName !in pluginMap) {
                    val versionInfo = if (depVersion != null) " (version: $depVersion)" else " (any version)"
                    throw IllegalStateException(
                        "Plugin '${plugin.info.name}' depends of '$depName'$versionInfo, but it is not present"
                    )
                }
            }
        }
    }

    private fun detectCircularDependencies(plugins: List<DiscoveredPlugin>, pluginMap: Map<String, DiscoveredPlugin>) {
        val white = plugins.map { it.info.name }.toMutableSet()
        val gray = mutableSetOf<String>()
        val black = mutableSetOf<String>()
        fun dfs(pluginName: String): Boolean {
            if (pluginName in gray)
                return true
            if (pluginName in black)
                return false
            white.remove(pluginName)
            gray.add(pluginName)
            pluginMap[pluginName]?.info?.dependencies?.forEach { (depName, _) ->
                if (dfs(depName)) {
                    throw IllegalStateException("Circular dependency betwen: $pluginName -> $depName")
                }
            }
            gray.remove(pluginName)
            black.add(pluginName)
            return false
        }
        while (white.isNotEmpty()) {
            dfs(white.first())
        }
    }

    private fun findCluster(pluginName: String, pluginMap: Map<String, DiscoveredPlugin>, visited: MutableSet<String>): Set<String> {
        if (pluginName in visited)
            return emptySet()
        visited.add(pluginName)
        val connected = mutableSetOf(pluginName)
        val plugin = pluginMap[pluginName] ?: return connected
        plugin.info.dependencies.forEach { (depName, _) ->
            connected.addAll(findCluster(depName, pluginMap, visited))
        }
        pluginMap.values.forEach { other ->
            if (other.info.dependencies.any { it.first == pluginName }) {
                connected.addAll(findCluster(other.info.name, pluginMap, visited))
            }
        }
        return connected
    }

    private fun loadPluginCluster(cluster: Set<String>, discoveredPlugins: List<DiscoveredPlugin>) {
        val clusterPlugins = discoveredPlugins.filter { it.info.name in cluster }
        val jarFiles = clusterPlugins.map { it.jarFile }
        val classLoader = PluginClassLoader(
            Thread.currentThread().contextClassLoader,
            jarFiles
        )
        val pluginCluster = PluginCluster(cluster, classLoader, jarFiles)
        pluginClusters.add(pluginCluster)

        val orderedCluster = orderPluginCluster(clusterPlugins)
        orderedCluster.forEach { discoveredPlugin ->
            try {
                val pluginInstance = loadPluginInstance(discoveredPlugin, classLoader)
                pluginInstance.info = discoveredPlugin.info
                val loadedPlugin = LoadedPlugin(
                    info = discoveredPlugin.info,
                    instance = pluginInstance,
                    classLoader = classLoader,
                    jarFile = discoveredPlugin.jarFile
                )
                loadedPlugins[discoveredPlugin.info.name.lowercase()] = loadedPlugin
                Console.log("Plugin '${discoveredPlugin.info.name}' instantiated")
            } catch (e: Exception) {
                Console.errLog("Could not instantiate plugin '${discoveredPlugin.info.name}': ${e.message}")
                throw e
            }
        }
    }

    private fun orderPluginCluster(plugins: List<DiscoveredPlugin>): List<DiscoveredPlugin> {
        val pluginMap = plugins.associateBy { it.info.name }
        val ordered = mutableListOf<DiscoveredPlugin>()
        val visited = mutableSetOf<String>()
        fun visit(pluginName: String) {
            if (pluginName in visited)
                return
            visited.add(pluginName)
            val plugin = pluginMap[pluginName] ?: return
            plugin.info.dependencies.forEach { (depName, _) ->
                visit(depName)
            }
            ordered.add(plugin)
        }
        plugins.forEach { visit(it.info.name) }
        return ordered
    }

    private fun loadPluginInstance(discoveredPlugin: DiscoveredPlugin, classLoader: PluginClassLoader): Plugin {
        val mainClass = getMainClass(discoveredPlugin.jarFile)
        val pluginClass = classLoader.loadClass(mainClass)
        val constructor = pluginClass.getDeclaredConstructor()
        val instance = constructor.newInstance()
        if (instance !is Plugin) {
            throw IllegalArgumentException("Main class for ${discoveredPlugin.info.name} does not implement the 'Plugin' interface.")
        }
        return instance
    }

    private fun getMainClass(jarFile: File): String {
        JarFile(jarFile).use { jar ->
            val entry = jar.getJarEntry(INFO_PATH)
                ?: throw IllegalArgumentException("plugin.yml not found")
            val config = YamlConfig.load(jar.getInputStream(entry))
            return config.getString("main-class")
                ?: throw IllegalArgumentException(" 'main-class' not found")
        }
    }

    private fun initializeAllPlugins() {
        Console.log("Initializing plugins...")
        val initializationOrder = getInitializationOrder()
        initializationOrder.forEach { pluginName ->
            val loadedPlugin = loadedPlugins[pluginName.lowercase()]
            if (loadedPlugin != null) {
                try {
                    Console.log("Initializing '${loadedPlugin.info.name}'")
                    loadedPlugin.instance.init()
                } catch (e: Exception) {
                    Console.errLog("Error initializing plugin '${loadedPlugin.info.name}': ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    fun shutdownAllPlugins() {
        Console.log("shutting down plugins...")
        val shutdownOrder = getInitializationOrder().reversed()
        shutdownOrder.forEach { pluginName ->
            val loadedPlugin = loadedPlugins[pluginName.lowercase()]
            if (loadedPlugin != null) {
                try {
                    Console.log("shutting down '${loadedPlugin.info.name}'")
                    loadedPlugin.instance.shutdown()
                } catch (e: Exception) {
                    Console.errLog("Error shutting plugin down '${loadedPlugin.info.name}': ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getInitializationOrder(): List<String> {
        val ordered = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        fun visit(pluginName: String) {
            if (pluginName in visiting) {
                throw IllegalStateException("Circular dependency on initialization: $pluginName")
            }
            if (pluginName in visited)
                return
            visiting.add(pluginName)
            val plugin = loadedPlugins[pluginName.lowercase()]
            if (plugin != null) {
                plugin.info.dependencies.forEach { (depName, _) ->
                    if (depName in loadedPlugins) {
                        visit(depName.lowercase())
                    }
                }
                visited.add(pluginName)
                ordered.add(pluginName)
            }
            visiting.remove(pluginName)
        }
        loadedPlugins.keys.forEach { pluginName ->
            if (pluginName !in visited) {
                visit(pluginName)
            }
        }
        return ordered
    }

    fun getPlugin(name: String): LoadedPlugin? {
        return loadedPlugins[name.lowercase()]
    }

    fun getAllPlugins(): Map<String, LoadedPlugin> {
        return loadedPlugins.toMap()
    }

    fun loadPluginByName(name: String): Boolean {
        val normalizedName = name.lowercase()
        if (loadedPlugins.containsKey(normalizedName)) {
            Console.errLog("Plugin '$name' is already loaded.")
            return false
        }
        val pluginsDir = File(PLUGINS_DIR)
        val jarFile = pluginsDir.listFiles { file -> file.extension == "jar" && file.nameWithoutExtension.equals(name, true) }?.firstOrNull()
        if (jarFile == null) {
            Console.errLog("Plugin jar for '$name' not found in plugins directory.")
            return false
        }
        val pluginInfo = try {
            readInfo(jarFile)
        } catch (e: Exception) {
            Console.errLog("Error reading plugin info for '$name': ${e.message}")
            return false
        }
        val classLoader = PluginClassLoader(Thread.currentThread().contextClassLoader, listOf(jarFile))
        val discoveredPlugin = DiscoveredPlugin(pluginInfo, jarFile)
        val pluginInstance = try {
            loadPluginInstance(discoveredPlugin, classLoader)
        } catch (e: Exception) {
            Console.errLog("Error loading plugin instance for '$name': ${e.message}")
            classLoader.close()
            return false
        }
        pluginInstance.info = pluginInfo
        val loadedPlugin = LoadedPlugin(
            info = pluginInfo,
            instance = pluginInstance,
            classLoader = classLoader,
            jarFile = jarFile
        )
        val registryKey = pluginInfo.name.lowercase()
        loadedPlugins[registryKey] = loadedPlugin
        try {
            Console.log("Initializing '${pluginInfo.name}'")
            pluginInstance.init()
        } catch (e: Exception) {
            Console.errLog("Error initializing plugin '${pluginInfo.name}': ${e.message}")
            loadedPlugins.remove(registryKey)
            classLoader.close()
            return false
        }
        Console.log("Plugin '${pluginInfo.name}' loaded dynamically.")
        return true
    }

    fun unloadPluginByName(name: String): Boolean {
        val normalizedName = name.lowercase()
        val loadedEntry = loadedPlugins.entries.find { it.key == normalizedName }
        val loaded = loadedEntry?.value
        if (loaded == null) {
            Console.errLog("Plugin '$name' is not loaded.")
            return false
        }

        val targetClassLoader = loaded.classLoader
        val pluginsToUnload = loadedPlugins.filter { it.value.classLoader === targetClassLoader }.toList()

        pluginsToUnload.forEach { (key, pluginToUnload) ->
            try {
                Console.log("Shutting down '${pluginToUnload.info.name}' (part of unload cluster)")
                pluginToUnload.instance.shutdown()
            } catch (e: Exception) {
                Console.errLog("Error shutting down plugin '${pluginToUnload.info.name}': ${e.message}")
            }
            loadedPlugins.remove(key)
        }

        try {
            targetClassLoader.close()
            Console.log("ClassLoader for cluster containing '$name' closed.")
        } catch (e: Exception) {
            Console.errLog("Error closing classloader for cluster containing '$name': ${e.message}")
        }

        Console.log("Plugin(s) '${pluginsToUnload.joinToString(", ") { it.second.info.name }}' unloaded dynamically (cluster unload).")
        return true
    }

    fun reloadPluginByName(name: String): Boolean {
        val normalizedName = name.lowercase()
        val loadedEntry = loadedPlugins.entries.find { it.key == normalizedName }
        val loaded = loadedEntry?.value
        if (loaded == null) {
            Console.errLog("Plugin '$name' is not loaded.")
            return false
        }

        val jarFileName = loaded.jarFile.nameWithoutExtension

        val unloadSuccess = unloadPluginByName(name)
        if (!unloadSuccess) {
            Console.errLog("Failed to unload plugin '$name' during reload.")
            return false
        }

        val loadSuccess = loadPluginByName(jarFileName)
        if (!loadSuccess) {
            Console.errLog("Failed to load plugin '$jarFileName' after unloading '$name'. Plugin is currently not loaded.")
            return false
        }

        return true
    }
}