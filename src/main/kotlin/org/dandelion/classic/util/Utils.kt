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

    //got this from https://github.com/BunnyNabbit/classicborne-server-protocol/blob/37f8664f0e7016f81e0c4664865f8971390b70c2/class/CodePage437.mjs#L30-L48
    private val cp437Mapping = arrayOf(
        " ", "☺", "☻", "♥", "♦", "♣", "♠", "•", "◘", "○", "◙", "♂", "♀", "♪", "♫", "☼",
        "►", "◄", "↕", "‼", "¶", "§", "▬", "↨", "↑", "↓", "→", "←", "∟", "↔", "▲", "▼",
        " ", "!", "\"", "#", "$", "%", "&", "'", "(", ")", "*", "+", ",", "-", ".", "/",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ":", ";", "<", "=", ">", "?",
        "@", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
        "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "[", "\\", "]", "^", "_",
        "`", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o",
        "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "{", "|", "}", "~", "⌂",
        "Ç", "ü", "é", "â", "ä", "à", "å", "ç", "ê", "ë", "è", "ï", "î", "ì", "Ä", "Å",
        "É", "æ", "Æ", "ô", "ö", "ò", "û", "ù", "ÿ", "Ö", "Ü", "ø", "£", "Ø", "×", "ƒ",
        "á", "í", "ó", "ú", "ñ", "Ñ", "ª", "º", "¿", "⌐", "¬", "½", "¼", "¡", "«", "»",
        "░", "▒", "▓", "│", "┤", "╡", "╢", "╖", "╕", "╣", "║", "╗", "╝", "╜", "╛", "┐",
        "└", "┴", "┬", "├", "─", "┼", "╞", "╟", "╚", "╔", "╩", "╦", "╠", "═", "╬", "╧",
        "╨", "╤", "╥", "╙", "╘", "╒", "╓", "╫", "╪", "┘", "┌", "█", "▄", "▌", "▐", "▀",
        "α", "ß", "Γ", "π", "Σ", "σ", "µ", "τ", "Φ", "Θ", "Ω", "δ", "∞", "φ", "ε", "∩",
        "≡", "±", "≥", "≤", "⌠", "⌡", "÷", "≈", "°", "∙", "·", "√", "ⁿ", "²", "■", " "
    )

    fun convertToCp437(str: String): ByteArray {
        return str.map { char ->
            when (char) {
                ' ' -> 0x20.toByte()
                else -> {
                    val index = cp437Mapping.indexOf(char.toString())
                    if (index != -1) {
                        index.toByte()
                    } else {
                        cp437Mapping.indexOf("?").toByte()
                    }
                }
            }
        }.toByteArray()
    }

    fun convertFromCp437(bytes: ByteArray): String {
        return bytes.joinToString("") { byte ->
            val index = byte.toInt() and 0xFF
            if (index < cp437Mapping.size) {
                cp437Mapping[index]
            } else {
                "?"
            }
        }.trimEnd(' ')
    }

    fun convertToCp437WithFallback(str: String, supportsFullCp437: Boolean): ByteArray {
        return if (supportsFullCp437) {
            convertToCp437(str)
        } else {
            str.map { char ->
                when (char) {
                    ' ' -> 0x20.toByte()
                    else -> {
                        val index = cp437Mapping.indexOf(char.toString())
                        if (index != -1 && index <= 127) {
                            index.toByte()
                        } else {
                            cp437Mapping.indexOf("?").toByte()
                        }
                    }
                }
            }.toByteArray()
        }
    }
}
