package org.fern.engine.editor

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean

abstract class EditorWindow(
    val id: String,
    open val title: String,
    open val closeable: Boolean = true,
    open val hasMenuBar: Boolean = false,
    open val hasContextMenu: Boolean = false,
    open val flags: Int = 0,
    open val category: String = "",
    open val includeInWindowMenu: Boolean = true
) {

    var isOpen = true

    open fun init() {}

    open fun update(dt: Float) {}

    open fun renderMenuBar() {}
    open fun renderContextMenu() {}

    abstract fun renderContent()

    open fun onCloseRequest(): Boolean = true

    open fun dispose() {}

    fun render() {
        if (!isOpen) return

        val finalFlags = if (hasMenuBar) flags or ImGuiWindowFlags.MenuBar else flags
        val imOpen = if (closeable) ImBoolean(true) else null

        val began = if (imOpen != null)
            ImGui.begin(title, imOpen, finalFlags)
        else
            ImGui.begin(title, finalFlags)
        if (began) {
            if (hasContextMenu && ImGui.beginPopupContextWindow()) {
                renderContextMenu()
                ImGui.endPopup()
            }
            if (hasMenuBar && ImGui.beginMenuBar()) {
                renderMenuBar()
                ImGui.endMenuBar()
            }
            renderContent()
        }
        ImGui.end()

        if (closeable && imOpen != null && !imOpen.get()) {
            if (onCloseRequest())
                isOpen = false
        }
    }

}
