package com.example.data

import com.example.model.GameSpeed
import com.example.model.ResolutionOption
import com.example.model.VideoSettings

object IniFileEngine {

    private val sectionHeaderRegex = Regex("""^\s*\[([^\]]+)\]\s*$""")

    fun parse(content: String): Map<String, Map<String, String>> {
        val result = linkedMapOf<String, MutableMap<String, String>>()
        var currentSection: String? = null

        content.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#")) {
                return@forEach
            }
            val headerMatch = sectionHeaderRegex.matchEntire(trimmed)
            if (headerMatch != null) {
                currentSection = headerMatch.groupValues[1].trim()
                result.getOrPut(currentSection!!) { linkedMapOf() }
            } else if (currentSection != null) {
                val eqIndex = trimmed.indexOf('=')
                if (eqIndex > 0) {
                    val key = trimmed.substring(0, eqIndex).trim()
                    val value = trimmed.substring(eqIndex + 1).trim()
                    result.getOrPut(currentSection!!) { linkedMapOf() }[key] = value
                }
            }
        }
        return result
    }

    fun getValue(
        parsed: Map<String, Map<String, String>>,
        section: String,
        key: String
    ): String? {
        val secEntry = parsed.entries.firstOrNull { it.key.equals(section, ignoreCase = true) } ?: return null
        return secEntry.value.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
    }

    fun extractVideoSettings(content: String, fallback: VideoSettings): VideoSettings {
        if (content.isBlank()) return fallback
        val parsed = parse(content)

        val width = getValue(parsed, "Video", "ScreenWidth")?.toIntOrNull()
        val height = getValue(parsed, "Video", "ScreenHeight")?.toIntOrNull()
        val resolution = if (width != null && height != null && width >= 640 && height >= 480) {
            ResolutionOption(width, height)
        } else {
            fallback.resolution
        }

        val stretch = parseYesNo(getValue(parsed, "Video", "StretchMovies")) ?: fallback.stretchMovies
        val hiRes = parseYesNo(getValue(parsed, "Video", "AllowHiResModes")) ?: fallback.allowHiResModes
        val backBuffer = parseYesNo(getValue(parsed, "Video", "VideoBackBuffer")) ?: fallback.videoBackBuffer
        val speed = GameSpeed.fromIniValue(getValue(parsed, "Settings", "GameSpeed")) ?: fallback.gameSpeed

        return fallback.copy(
            resolution = resolution,
            gameSpeed = speed,
            stretchMovies = stretch,
            allowHiResModes = hiRes,
            videoBackBuffer = backBuffer
        )
    }

    fun applyVideoSettings(existingContent: String, settings: VideoSettings): String {
        val updates = linkedMapOf(
            "Video" to linkedMapOf(
                "ScreenWidth" to settings.resolution.width.toString(),
                "ScreenHeight" to settings.resolution.height.toString(),
                "StretchMovies" to if (settings.stretchMovies) "yes" else "no",
                "AllowHiResModes" to if (settings.allowHiResModes) "yes" else "no",
                "VideoBackBuffer" to if (settings.videoBackBuffer) "yes" else "no"
            ),
            "Settings" to linkedMapOf(
                "GameSpeed" to settings.gameSpeed.iniValue
            )
        )
        return writePrivateProfileSections(existingContent, updates)
    }

    fun writePrivateProfileSections(
        existingContent: String,
        updates: Map<String, Map<String, String>>
    ): String {
        val lines = if (existingContent.isEmpty()) {
            mutableListOf()
        } else {
            existingContent.replace("\r\n", "\n").split("\n").toMutableList()
        }

        val pendingUpdates = linkedMapOf<String, MutableMap<String, String>>()
        updates.forEach { (sec, keys) ->
            pendingUpdates[sec] = keys.toMutableMap()
        }

        val output = mutableListOf<String>()
        var activeSectionKey: String? = null

        fun flushRemainingKeysForActiveSection() {
            val secKey = activeSectionKey ?: return
            val remaining = pendingUpdates[secKey] ?: return
            if (remaining.isNotEmpty()) {
                // Insert before trailing blank lines of the section
                var insertIdx = output.size
                while (insertIdx > 0 && output[insertIdx - 1].isBlank()) {
                    insertIdx--
                }
                val newLines = remaining.entries.map { (k, v) -> "$k=$v" }
                output.addAll(insertIdx, newLines)
                remaining.clear()
            }
            pendingUpdates.remove(secKey)
            activeSectionKey = null
        }

        for (line in lines) {
            val trimmed = line.trim()
            val headerMatch = sectionHeaderRegex.matchEntire(trimmed)
            if (headerMatch != null) {
                flushRemainingKeysForActiveSection()
                val foundName = headerMatch.groupValues[1].trim()
                activeSectionKey = pendingUpdates.keys.firstOrNull { it.equals(foundName, ignoreCase = true) }
                output.add(line)
            } else {
                val secKey = activeSectionKey
                if (secKey != null && !trimmed.startsWith(";") && !trimmed.startsWith("#")) {
                    val eqIdx = trimmed.indexOf('=')
                    if (eqIdx > 0) {
                        val keyName = trimmed.substring(0, eqIdx).trim()
                        val map = pendingUpdates[secKey]
                        val matchingKey = map?.keys?.firstOrNull { it.equals(keyName, ignoreCase = true) }
                        if (matchingKey != null) {
                            val newVal = map.remove(matchingKey)
                            output.add("$matchingKey=$newVal")
                            continue
                        }
                    }
                }
                output.add(line)
            }
        }
        flushRemainingKeysForActiveSection()

        // Append any sections that did not exist in the original file
        pendingUpdates.forEach { (sectionName, entries) ->
            if (entries.isNotEmpty()) {
                if (output.isNotEmpty() && output.last().isNotBlank()) {
                    output.add("")
                }
                output.add("[$sectionName]")
                entries.forEach { (k, v) ->
                    output.add("$k=$v")
                }
            }
        }

        // Remove single trailing empty artifact if original ended with newline, then normalize CRLF
        while (output.isNotEmpty() && output.last().isEmpty()) {
            output.removeAt(output.lastIndex)
        }
        return if (output.isEmpty()) "" else output.joinToString("\r\n") + "\r\n"
    }

    private fun parseYesNo(value: String?): Boolean? {
        return when (value?.trim()?.lowercase()) {
            "yes", "true", "1" -> true
            "no", "false", "0" -> false
            else -> null
        }
    }
}
