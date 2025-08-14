package org.dandelion.classic.plugins.model

import java.io.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import org.dandelion.classic.plugins.manager.PluginRegistry

abstract class Plugin {
    var info: PluginInfo = PluginInfo.empty()

    abstract fun init()

    abstract fun shutdown()

    fun getDataDir(): File {
        val pluginsDir = File(PluginRegistry.PLUGINS_DIR)
        val dataDir = File(pluginsDir, info.name)
        if (!dataDir.exists()) {
            dataDir.mkdirs()
        }
        return dataDir
    }

    fun copyResource(resourcePath: String, targetPath: String) {
        val loadedPlugin =
            PluginRegistry.getPlugin(info.name)
                ?: throw IllegalStateException(
                    "Plugin ${info.name} is not loaded in registry"
                )

        var input: InputStream? = null
        var output: FileOutputStream? = null
        try {
            val jarFile = JarFile(loadedPlugin.jarFile)
            val jarEntry: JarEntry =
                jarFile.getJarEntry(resourcePath)
                    ?: throw IllegalArgumentException(
                        "Resource not found in JAR: $resourcePath"
                    )

            input = jarFile.getInputStream(jarEntry)
            val targetFile = File(targetPath)
            targetFile.parentFile?.mkdirs()
            output = FileOutputStream(targetFile)

            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            output.flush()
        } catch (e: IOException) {
            throw RuntimeException(
                "Failed to copy resource $resourcePath from JAR to $targetPath",
                e,
            )
        } finally {
            try {
                input?.close()
            } catch (e: IOException) {
                System.err.println("Could not close input stream: ${e.message}")
            }
            try {
                output?.close()
            } catch (e: IOException) {
                System.err.println(
                    "Could not close output stream: ${e.message}"
                )
            }
        }
    }

    /** copies resource only if it doesnt exist in target path */
    fun deployResource(resourcePath: String, targetPath: String) {
        val targetFile = File(targetPath)
        if (targetFile.exists()) {
            return
        }
        copyResource(resourcePath, targetPath)
    }
}
