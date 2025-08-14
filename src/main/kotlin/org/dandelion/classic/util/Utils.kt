package org.dandelion.classic.util

import java.io.File
import java.io.FileOutputStream

object Utils {
    fun copyResourceTo(resourcePath: String, targetPath: String) {
        val inputStream =
            Utils::class.java.classLoader.getResourceAsStream(resourcePath)
                ?: throw IllegalArgumentException(
                    "Resource not found: $resourcePath"
                )
        val outputFile = File(targetPath)
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { outputStream ->
            inputStream.use { it.copyTo(outputStream) }
        }
    }
}
