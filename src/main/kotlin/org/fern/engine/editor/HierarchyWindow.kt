package org.fern.engine.editor

import imgui.ImGui
import imgui.flag.ImGuiKey
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiSelectableFlags
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import org.fern.engine.scene.GameObject
import org.fern.engine.scene.Scene
import java.util.IdentityHashMap

class HierarchyWindow(
    private val scene: Scene,
    private val onSelect: (GameObject?) -> Unit = {}
) : EditorWindow(
    id = "hierarchy",
    title = "Hierarchy",
    closeable = true,
    hasMenuBar = false,
    hasContextMenu = true,
    flags = ImGuiWindowFlags.None,
    category = "Scene"
) {

    var selected: GameObject? = null
        private set

    private val renameBuffers = IdentityHashMap<GameObject, ImString>()
    private val isRenaming = IdentityHashMap<GameObject, Boolean>()

    private val renameRequests = IdentityHashMap<GameObject, Boolean>()

    override fun renderContent() {
        if (ImGui.beginPopupContextWindow()) {
            if (ImGui.menuItem("Create Empty GameObject")) {
                val go = scene.createGameObject("GameObject")
                select(go)
            }
            ImGui.endPopup()
        }

        if (ImGui.isWindowFocused() && selected != null) {
            if (ImGui.isKeyPressed(ImGuiKey.F2, false)) {
                val sel = selected!!
                isRenaming[sel] = true
                renameBuffers[sel] = ImString(sel.name, 128)
                renameRequests[sel] = true
            }
            if (ImGui.isKeyPressed(ImGuiKey.Delete, false)) {
                val sel = selected!!
                scene.destroy(sel)
                select(null)
                return
            }
        }

        scene.roots.forEach { drawNodeRecursive(it) }
    }

    private fun drawNodeRecursive(go: GameObject) {
        ImGui.pushID(go.hashCode())

        val hasChildren = go.children.isNotEmpty()
        val treeFlags =
            ImGuiTreeNodeFlags.OpenOnArrow or
            ImGuiTreeNodeFlags.SpanAvailWidth

        val renaming = isRenaming[go] == true

        if (hasChildren) {
            val opened = if (renaming)
                ImGui.treeNodeEx("", treeFlags)
            else
                ImGui.treeNodeEx(go.name, treeFlags)

            if (ImGui.isItemClicked(ImGuiMouseButton.Left))
                select(go)

            if (renaming) {
                val buf = renameBuffers.computeIfAbsent(go) { ImString(go.name, 128) }
                ImGui.sameLine()
                ImGui.pushID("rename_${go.hashCode()}")

                if (renameRequests.remove(go) == true)
                    ImGui.setKeyboardFocusHere()

                ImGui.inputText("", buf)
                if (ImGui.isItemDeactivated()) {
                    val newName = buf.get().trim()
                    if (newName.isNotEmpty())
                        go.name = newName
                    isRenaming[go] = false
                }
                ImGui.popID()
            }

            handleNodeContext(go)

            if (opened) {
                go.children.forEach { drawNodeRecursive(it) }
                ImGui.treePop()
            }
        } else {
            if (renaming) {
                val buf = renameBuffers.computeIfAbsent(go) { ImString(go.name, 128) }
                ImGui.pushID("rename_${go.hashCode()}")

                if (renameRequests.remove(go) == true)
                    ImGui.setKeyboardFocusHere()

                ImGui.inputText("", buf)
                if (ImGui.isItemDeactivated()) {
                    val newName = buf.get().trim()
                    if (newName.isNotEmpty())
                        go.name = newName
                    isRenaming[go] = false
                }
                ImGui.popID()
            } else {
                if (ImGui.selectable(go.name, selected === go))
                    select(go)
            }

            handleNodeContext(go)
        }

        ImGui.popID()
    }

    private fun handleNodeContext(go: GameObject) {
        if (ImGui.beginPopupContextItem()) {
            if (ImGui.menuItem("Create Child")) {
                val child = scene.createGameObject("GameObject", go)
                select(child)
            }
            if (ImGui.menuItem("Rename")) {
                isRenaming[go] = true
                renameBuffers[go] = ImString(go.name, 128)
                renameRequests[go] = true
            }
            if (ImGui.menuItem("Delete")) {
                scene.destroy(go)
                if (selected === go)
                    select(null)
                ImGui.endPopup()
                return
            }
            ImGui.endPopup()
        }

        if (ImGui.isItemClicked(ImGuiMouseButton.Left) && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left))
            select(go)
    }

    private fun select(go: GameObject?) {
        selected = go
        onSelect(go)
    }

}