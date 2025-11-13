package org.fern.engine.editor

import imgui.ImGui
import imgui.flag.ImGuiDockNodeFlags
import java.io.File

class EditorSystem(
    persistenceFile: File? = null,
    private val enableDockSpace: Boolean = true,
    private val dockFlags: Int = ImGuiDockNodeFlags.PassthruCentralNode,
    private val autoMainMenuBar: Boolean = true
) {

    val registry = EditorWindowRegistry()

    private val persistence: EditorPersistence? = persistenceFile?.let { EditorPersistence(it) }

    private var isInitialized = false

    fun init() {
        if (isInitialized) return

        persistence?.load(registry)
        isInitialized = true
    }

    fun update(dt: Float) {
        if (!isInitialized)
            init()
        registry.all().forEach { it.update(dt) }
    }

    fun render() {
        if (enableDockSpace)
            ImGui.dockSpaceOverViewport(dockFlags)
        if (autoMainMenuBar)
            renderMainMenuBar()
        registry.all().forEach { it.render() }
    }

    private fun renderMainMenuBar() {
        if (ImGui.beginMainMenuBar()) {
            renderWindowMenu()
            ImGui.endMainMenuBar()
        }
    }

    private fun renderWindowMenu() {
        if (!ImGui.beginMenu("Window")) return

        val grouped = registry.all()
            .filter { it.includeInWindowMenu }
            .groupBy { it.category.trim() }

        grouped[""]?.let { list ->
            list.forEach { win ->
                val toggled = ImGui.menuItem(win.title, "", win.isOpen)
                if (toggled)
                    win.isOpen = !win.isOpen
            }
            if (grouped.size > 1)
                ImGui.separator()
        }

        grouped
            .filterKeys { it.isNotEmpty() }
            .toSortedMap()
            .forEach { (cat, list) ->
                if (ImGui.beginMenu(cat)) {
                    list.forEach { win ->
                        val toggled = ImGui.menuItem(win.title, "", win.isOpen)
                        if (toggled)
                            win.isOpen = !win.isOpen
                    }
                    ImGui.endMenu()
                }
            }

        ImGui.endMenu()
    }

    fun shutdown() {
        persistence?.save(registry)
        registry.dispose()
    }

}