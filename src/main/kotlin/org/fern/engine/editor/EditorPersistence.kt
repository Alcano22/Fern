package org.fern.engine.editor

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.fern.engine.logger.logger
import java.io.File

class EditorPersistence(
    private val file: File
) {

    @Serializable
    data class WindowStateDTO(
        val id: String,
        val open: Boolean,
        val custom: Map<String, String> = emptyMap()
    )

    @Serializable
    data class EditorStateDTO(
        val windows: List<WindowStateDTO> = emptyList()
    )

    companion object {
        private val logger = logger()
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun save(registry: EditorWindowRegistry) {
        val dto = EditorStateDTO(
            registry.all().map { window ->
                WindowStateDTO(
                    window.id,
                    window.isOpen,
                    emptyMap()
                )
            }
        )

        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(dto))
        }.onFailure { t ->
            logger.error { "Failed to save state to '${file.absolutePath}': ${t.message}" }
        }
    }

    fun load(registry: EditorWindowRegistry) {
        if (!file.exists()) return

        runCatching {
            val content = file.readText()
            val dto = json.decodeFromString<EditorStateDTO>(content)
            dto.windows.forEach { ws ->
                registry.getOrCreate(ws.id)?.let { win ->
                    win.isOpen = ws.open
                }
            }
        }.onFailure { t ->
            logger.error { "Failed to load state from '${file.absolutePath}': ${t.message}" }
        }
    }

}