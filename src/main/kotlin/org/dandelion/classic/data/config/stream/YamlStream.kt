package org.dandelion.classic.data.config.stream

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter

class YamlStream(private val fileName: String) {
    private var data: MutableMap<String, Any?> = mutableMapOf()

    init {
        load()
    }

    fun load() {
        val file = File(fileName)
        file.parentFile?.mkdirs()
        if (!file.exists()) {
            file.createNewFile()
            save()
        }
        data = try {
            FileInputStream(file).use { inputStream ->
                val yaml = Yaml()
                val loaded = yaml.load<Map<String, Any?>>(inputStream)
                loaded?.toMutableMap() ?: mutableMapOf()
            }
        } catch (_: Exception) {
            mutableMapOf()
        }
    }

    fun save() {
        val file = File(fileName)
        file.parentFile?.mkdirs()
        if (!file.exists()) file.createNewFile()
        val option = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        }
        val yaml = Yaml(option)
        FileWriter(fileName).use { writer ->
            yaml.dump(data, writer)
        }
    }

    fun get(key: String, default: Any? = null): Any? {
        return getValueByPath(key) ?: default
    }

    fun getString(key: String, default: String? = null): String? = get(key, default) as? String ?: default
    fun getInt(key: String, default: Int? = null): Int? = when(val v = get(key, default)) {
        is Int -> v
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: default
        else -> default
    }
    fun getBoolean(key: String, default: Boolean? = null): Boolean? = when(val v = get(key, default)) {
        is Boolean -> v
        is String -> v.toBooleanStrictOrNull() ?: default
        else -> default
    }
    fun getList(key: String): List<Any?> = get(key) as? List<Any?> ?: emptyList()
    fun getMap(key: String): Map<String, Any?> = get(key) as? Map<String, Any?> ?: emptyMap()

    fun set(key: String, value: Any?) {
        setValueByPath(key, value)
    }

    fun getSection(key: String): YamlStreamSection = YamlStreamSection(getMap(key))

    private fun getValueByPath(path: String): Any? {
        val keys = path.split('.')
        var current: Any? = data
        for (k in keys) {
            if (current is Map<*, *>) {
                current = current[k]
            } else {
                return null
            }
        }
        return current
    }

    private fun setValueByPath(path: String, value: Any?) {
        val keys = path.split('.')
        var current: MutableMap<String, Any?> = data
        for (i in 0 until keys.size - 1) {
            val k = keys[i]
            val next = current[k]
            if (next !is MutableMap<*, *>) {
                val newMap = mutableMapOf<String, Any?>()
                current[k] = newMap
                current = newMap
            } else {
                @Suppress("UNCHECKED_CAST")
                current = next as MutableMap<String, Any?>
            }
        }
        current[keys.last()] = value
    }

    class YamlStreamSection(private val section: Map<String, Any?>) {
        fun get(key: String, default: Any? = null): Any? = section[key] ?: default
        fun getString(key: String, default: String? = null): String? = get(key, default) as? String ?: default
        fun getInt(key: String, default: Int? = null): Int? = when(val v = get(key, default)) {
            is Int -> v
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: default
            else -> default
        }
        fun getBoolean(key: String, default: Boolean? = null): Boolean? = when(val v = get(key, default)) {
            is Boolean -> v
            is String -> v.toBooleanStrictOrNull() ?: default
            else -> default
        }
        fun getList(key: String): List<Any?> = get(key) as? List<Any?> ?: emptyList()
        fun getMap(key: String): Map<String, Any?> = get(key) as? Map<String, Any?> ?: emptyMap()
    }
}

