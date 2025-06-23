package org.dandelion.classic.util

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class YamlConfig {

    private var root: MutableMap<String, Any> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    private fun getMap(path: String): Map<String, Any>? {
        val keys = path.split('.')
        var currentMap: Map<String, Any>? = root
        for (i in 0 until keys.size - 1) {
            currentMap = currentMap?.get(keys[i]) as? Map<String, Any>
        }
        return currentMap
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOrCreateMap(path: String): MutableMap<String, Any> {
        val keys = path.split('.')
        var currentMap: MutableMap<String, Any> = root
        for (key in keys) {
            currentMap = currentMap.computeIfAbsent(key) { mutableMapOf<String, Any>() } as MutableMap<String, Any>
        }
        return currentMap
    }


    fun get(path: String, default: Any? = null): Any? {
        val map = getMap(path)
        val key = path.split('.').last()
        return map?.get(key) ?: default
    }

    fun getString(path: String): String? {
        val map = getMap(path)
        val key = path.split('.').last()
        return map?.get(key) as? String
    }

    fun getString(path: String, default: String): String {
        return getString(path) ?: default
    }

    fun getInt(path: String): Int? {
        val map = getMap(path)
        val key = path.split('.').last()
        return (map?.get(key) as? Number)?.toInt()
    }

    fun getInt(path: String, default: Int): Int {
        return getInt(path) ?: default
    }

    fun getLong(path: String): Long? {
        val map = getMap(path)
        val key = path.split('.').last()
        return (map?.get(key) as? Number)?.toLong()
    }

    fun getLong(path: String, default: Long): Long {
        return getLong(path) ?: default
    }

    fun getDouble(path: String): Double? {
        val map = getMap(path)
        val key = path.split('.').last()
        return (map?.get(key) as? Number)?.toDouble()
    }

    fun getDouble(path: String, default: Double): Double {
        return getDouble(path) ?: default
    }

    fun getBoolean(path: String): Boolean? {
        val map = getMap(path)
        val key = path.split('.').last()
        return map?.get(key) as? Boolean
    }

    fun getBoolean(path: String, default: Boolean): Boolean {
        return getBoolean(path) ?: default
    }

    @Suppress("UNCHECKED_CAST")
    fun getStringList(path: String): List<String>? {
        val map = getMap(path)
        val key = path.split('.').last()
        return map?.get(key) as? List<String>
    }

    fun getStringList(path: String, default: List<String>): List<String> {
        return getStringList(path) ?: default
    }

    fun getSection(path: String): YamlConfig? {
        val map = getMap(path)
        val key = path.split('.').last()
        val sectionMap = map?.get(key) as? Map<String, Any> ?: return null
        val config = YamlConfig()
        config.root = sectionMap.toMutableMap()
        return config
    }

    fun set(path: String, value: Any) {
        val keys = path.split('.')
        val key = keys.last()
        val mapPath = keys.dropLast(1).joinToString(".")
        val map = if (mapPath.isEmpty()) root else getOrCreateMap(mapPath)
        map[key] = value
    }

    fun setString(path: String, value: String) = set(path, value)
    fun setInt(path: String, value: Int) = set(path, value)
    fun setLong(path: String, value: Long) = set(path, value)
    fun setDouble(path: String, value: Double) = set(path, value)
    fun setBoolean(path: String, value: Boolean) = set(path, value)
    fun setStringList(path: String, value: List<String>) = set(path, value)

    fun save(file: File) {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.isPrettyFlow = true

        val yaml = Yaml(options)
        file.writeText(yaml.dump(root))
    }

    companion object {
        fun load(file: File): YamlConfig {
            return load(FileInputStream(file))
        }

        fun load(inputStream: InputStream): YamlConfig {
            val yaml = Yaml()
            val config = YamlConfig()
            inputStream.use {
                config.root = yaml.load(it) ?: mutableMapOf()
            }
            return config
        }
    }
}
