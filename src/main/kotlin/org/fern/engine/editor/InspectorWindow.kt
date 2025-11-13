package org.fern.engine.editor

import imgui.ImGui
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import imgui.type.ImString
import org.fern.engine.imgui.ImGuiEx
import org.fern.engine.logger.logger
import org.fern.engine.scene.GameObject
import org.fern.engine.scene.Scene
import org.fern.engine.scene.component.Component
import org.fern.engine.scene.component.ComponentRegistry
import org.fern.engine.scene.component.Transform

interface Inspectable {
    val title: String

    fun draw()
}

class InspectorWindow : EditorWindow(
    id = "inspector",
    title = "Inspector",
    closeable = true,
    hasMenuBar = false,
    hasContextMenu = false,
    flags = ImGuiWindowFlags.None,
    category = "Scene"
) {

    private var target: Inspectable? = null

    fun select(go: Inspectable?) {
        target = go
    }

    override fun init() {
        ComponentRegistry.scanAndRegister("org.fern.engine.scene.component")
    }

    override fun renderContent() {
        if (target == null) {
            ImGui.textDisabled("Nothing selected")
            return
        }

        ImGui.separator()

        target!!.draw()
    }

}