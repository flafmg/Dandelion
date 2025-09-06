package org.dandelion.server.util

import com.google.gson.*
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class JsonConfig {
    var root: JsonObject = JsonObject()
    private var originFile: File? = null
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun getElement(path: String): JsonElement? {
        if (path.isEmpty()) return root
        val keys = path.split('.')
        var currentElement: JsonElement = root
        for (key in keys) {
            if (currentElement.isJsonObject) {
                currentElement =
                    currentElement.asJsonObject.get(key) ?: return null
            } else {
                return null
            }
        }
        return currentElement
    }

    fun getOrCreateObject(path: String): JsonObject {
        if (path.isEmpty()) return root
        val keys = path.split('.')
        var currentObject: JsonObject = root
        for (key in keys) {
            val nextElement = currentObject.get(key)
            currentObject =
                if (nextElement?.isJsonObject == true) {
                    nextElement.asJsonObject
                } else {
                    val newObject = JsonObject()
                    currentObject.add(key, newObject)
                    newObject
                }
        }
        return currentObject
    }

    fun get(path: String, default: Any? = null): Any? {
        val element = getElement(path) ?: return default
        return when {
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                when {
                    primitive.isString -> primitive.asString
                    primitive.isNumber -> primitive.asNumber
                    primitive.isBoolean -> primitive.asBoolean
                    else -> default
                }
            }
            element.isJsonArray -> {
                val array = element.asJsonArray
                array.map { arrayElement ->
                    when {
                        arrayElement.isJsonPrimitive -> {
                            val primitive = arrayElement.asJsonPrimitive
                            when {
                                primitive.isString -> primitive.asString
                                primitive.isNumber -> primitive.asNumber
                                primitive.isBoolean -> primitive.asBoolean
                                else -> arrayElement.toString()
                            }
                        }
                        else -> arrayElement.toString()
                    }
                }
            }
            else -> default
        }
    }

    fun getString(path: String): String? {
        val element = getElement(path)
        return if (
            element?.isJsonPrimitive == true && element.asJsonPrimitive.isString
        ) {
            element.asString
        } else null
    }

    fun getString(path: String, default: String): String {
        return getString(path) ?: default
    }

    fun getInt(path: String): Int? {
        val element = getElement(path)
        return if (
            element?.isJsonPrimitive == true && element.asJsonPrimitive.isNumber
        ) {
            element.asInt
        } else null
    }

    fun getInt(path: String, default: Int): Int {
        return getInt(path) ?: default
    }

    fun getLong(path: String): Long? {
        val element = getElement(path)
        return if (
            element?.isJsonPrimitive == true && element.asJsonPrimitive.isNumber
        ) {
            element.asLong
        } else null
    }

    fun getLong(path: String, default: Long): Long {
        return getLong(path) ?: default
    }

    fun getDouble(path: String): Double? {
        val element = getElement(path)
        return if (
            element?.isJsonPrimitive == true && element.asJsonPrimitive.isNumber
        ) {
            element.asDouble
        } else null
    }

    fun getDouble(path: String, default: Double): Double {
        return getDouble(path) ?: default
    }

    fun getBoolean(path: String): Boolean? {
        val element = getElement(path)
        return if (
            element?.isJsonPrimitive == true &&
                element.asJsonPrimitive.isBoolean
        ) {
            element.asBoolean
        } else null
    }

    fun getBoolean(path: String, default: Boolean): Boolean {
        return getBoolean(path) ?: default
    }

    fun getStringList(path: String): List<String>? {
        val element = getElement(path)
        return if (element?.isJsonArray == true) {
            element.asJsonArray.mapNotNull { arrayElement ->
                if (
                    arrayElement.isJsonPrimitive &&
                        arrayElement.asJsonPrimitive.isString
                ) {
                    arrayElement.asString
                } else null
            }
        } else null
    }

    fun getStringList(path: String, default: List<String>): List<String> {
        return getStringList(path) ?: default
    }

    fun getArray(path: String): JsonArray? {
        val element = getElement(path)
        return if (element?.isJsonArray == true) element.asJsonArray else null
    }

    fun getOrCreateArray(path: String): JsonArray {
        val element = getElement(path)
        return if (element?.isJsonArray == true) {
            element.asJsonArray
        } else {
            val array = JsonArray()
            set(path, array)
            array
        }
    }

    fun getSection(path: String): JsonConfig? {
        val element = getElement(path)
        return if (element?.isJsonObject == true) {
            val config = JsonConfig()
            config.root = element.asJsonObject
            config
        } else null
    }

    fun getOrCreateSection(path: String): JsonConfig {
        val obj = getOrCreateObject(path)
        val config = JsonConfig()
        config.root = obj
        return config
    }

    fun set(path: String, value: Any) {
        if (path.isEmpty()) {
            throw IllegalArgumentException("Cannot set value at empty path ''")
        }
        val keys = path.split('.')
        val key = keys.last()
        val parentPath = keys.dropLast(1).joinToString(".")
        val parentObject =
            if (parentPath.isEmpty()) root else getOrCreateObject(parentPath)

        when (value) {
            is String -> parentObject.addProperty(key, value)
            is Int -> parentObject.addProperty(key, value)
            is Long -> parentObject.addProperty(key, value)
            is Double -> parentObject.addProperty(key, value)
            is Boolean -> parentObject.addProperty(key, value)
            is JsonElement -> parentObject.add(key, value)
            is List<*> -> {
                val array = JsonArray()
                value.forEach { item ->
                    when (item) {
                        is String -> array.add(item)
                        is Int -> array.add(item)
                        is Long -> array.add(item)
                        is Double -> array.add(item)
                        is Boolean -> array.add(item)
                        else -> array.add(item.toString())
                    }
                }
                parentObject.add(key, array)
            }
            else -> parentObject.addProperty(key, value.toString())
        }
    }

    fun setString(path: String, value: String) = set(path, value)

    fun setInt(path: String, value: Int) = set(path, value)

    fun setLong(path: String, value: Long) = set(path, value)

    fun setDouble(path: String, value: Double) = set(path, value)

    fun setBoolean(path: String, value: Boolean) = set(path, value)

    fun setStringList(path: String, value: List<String>) = set(path, value)

    fun save(file: File) {
        try {
            FileWriter(file).use { writer -> gson.toJson(root, writer) }
        } catch (e: Exception) {
            throw RuntimeException("Failed to save JSON file: ${file.path}", e)
        }
    }

    fun save() {
        val fileToSave =
            originFile
                ?: throw IllegalStateException(
                    "Config was not loaded from a file, it cannot be saved without a path!"
                )
        save(fileToSave)
    }

    fun has(path: String): Boolean {
        return getElement(path) != null
    }

    companion object {
        fun load(path: String): JsonConfig {
            return load(File(path))
        }

        fun load(file: File): JsonConfig {
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
                file.writeText("{}") // create empty JSON object
            }

            val config = JsonConfig()
            config.originFile = file

            try {
                FileReader(file).use { reader ->
                    val element = JsonParser.parseReader(reader)
                    config.root =
                        if (element.isJsonObject) element.asJsonObject
                        else JsonObject()
                }
            } catch (e: Exception) {
                config.root = JsonObject()
            }
            return config
        }

        fun loadArray(path: String): List<JsonConfig> {
            return loadArray(File(path))
        }

        fun loadArray(file: File): List<JsonConfig> {
            if (!file.exists()) {
                return emptyList()
            }

            return try {
                FileReader(file).use { reader ->
                    val element = JsonParser.parseReader(reader)
                    if (element.isJsonArray) {
                        element.asJsonArray.mapNotNull { arrayElement ->
                            if (arrayElement.isJsonObject) {
                                val config = JsonConfig()
                                config.root = arrayElement.asJsonObject
                                config
                            } else null
                        }
                    } else {
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
