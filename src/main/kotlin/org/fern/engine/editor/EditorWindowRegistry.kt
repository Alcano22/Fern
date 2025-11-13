package org.fern.engine.editor

import org.fern.engine.logger.logger
import java.util.concurrent.ConcurrentHashMap

class EditorWindowRegistry {

    companion object {
        private val logger = logger()
    }

    private val windows = LinkedHashMap<String, EditorWindow>()
    private val factories = ConcurrentHashMap<String, () -> EditorWindow>()

    fun register(window: EditorWindow): EditorWindow? {
        if (windows.containsKey(window.id)) {
            logger.warn {
                val name = window::class.simpleName!!
                "Already registered: $name"
            }
            return null
        }

        windows[window.id] = window
        window.init()
        return window
    }

    fun registerFactory(id: String, factory: () -> EditorWindow) {
        factories[id] = factory
    }

    fun getOrCreate(id: String): EditorWindow? {
        val existing = windows[id]
        if (existing != null)
            return existing
        val factory = factories[id] ?: return null
        val created = factory()
        register(created)
        return created
    }

    fun get(id: String) = windows[id]

    fun all() = windows.values

    fun show(id: String) {
        getOrCreate(id)?.let { it.isOpen = true }
    }

    fun hide(id: String) {
        get(id)?.let { it.isOpen = false }
    }

    fun toggle(id: String) {
        getOrCreate(id)?.let { it.isOpen = !it.isOpen }
    }

    fun dispose() {
        windows.values.forEach { it.dispose() }
        windows.clear()
        factories.clear()
    }

}