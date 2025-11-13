package org.fern.engine.imgui

import imgui.ImGui
import org.fern.engine.util.Action

object ImGuiAssetDnD {

    const val TYPE_TEXTURE = "texture"
    const val TYPE_SCRIPT  = "script"

    fun sourceTexture(assetRelativePath: String, preview: Action? = null) {
        if (!ImGui.beginDragDropSource()) return

        ImGui.setDragDropPayload(TYPE_TEXTURE, assetRelativePath)
        preview?.invoke()
        ImGui.textUnformatted(assetRelativePath)
        ImGui.endDragDropSource()
    }

    fun sourceScript(assetRelativePath: String, preview: Action? = null) {
        if (!ImGui.beginDragDropSource()) return

        ImGui.setDragDropPayload(TYPE_SCRIPT, assetRelativePath)
        preview?.invoke()
        ImGui.textUnformatted(assetRelativePath)
        ImGui.endDragDropSource()
    }

    inline fun acceptTexture(crossinline onAccept: (String) -> Unit) {
        if (!ImGui.beginDragDropTarget()) return

        val path = ImGui.acceptDragDropPayload<String>(TYPE_TEXTURE)
        if (path != null)
            onAccept(path)
        ImGui.endDragDropTarget()
    }

    inline fun acceptScript(crossinline onAccept: (String) -> Unit) {
        if (!ImGui.beginDragDropTarget()) return

        val path = ImGui.acceptDragDropPayload<String>(TYPE_SCRIPT)
        if (path != null)
            onAccept(path)
        ImGui.endDragDropTarget()
    }

}