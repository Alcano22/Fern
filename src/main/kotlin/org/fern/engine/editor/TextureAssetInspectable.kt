package org.fern.engine.editor

import imgui.ImGui
import imgui.type.ImString
import org.fern.engine.imgui.ImGuiEx
import org.fern.engine.renderer.Texture
import org.fern.engine.renderer.TextureLibrary
import org.fern.engine.resource.AssetMetaService
import org.fern.engine.resource.TextureMeta
import kotlin.math.max

class TextureAssetInspectable(
    private val assetRelativePath: String
) : Inspectable {

    override val title = assetRelativePath

    private var meta = AssetMetaService.loadTextureMeta(assetRelativePath) ?: TextureMeta()

    override fun draw() {
        ImGuiEx.disabled {
            ImGui.inputText("##Path", ImString(assetRelativePath))
        }

        ImGui.separator()

        drawPreviewSlot()

        ImGui.separator()
        drawMetaControls()
    }

    private fun drawPreviewSlot() {
        val frameH = ImGui.getFrameHeight()
        val slotSize = max(92f, frameH * 3.2f)
        val rounding = 5f

        val pMin = ImGui.getCursorScreenPos()
        val pMaxX = pMin.x + slotSize
        val pMaxY = pMin.y + slotSize

        ImGui.invisibleButton("##tex_asset_slot", slotSize, slotSize)
        val dl = ImGui.getWindowDrawList()

        val filepath = "user:$assetRelativePath"
        val tex = runCatching { TextureLibrary.get(filepath) }.getOrNull()
        val texId = runCatching { tex?.getId() ?: 0 }.getOrDefault(0)

        if (texId != 0) {
            dl.addImage(texId.toLong(), pMin.x, pMin.y, pMaxX, pMaxY, 0f, 1f, 1f, 0f)
            val borderCol = ImGui.getColorU32(1f, 1f, 1f, 0.18f)
            dl.addRect(pMin.x, pMin.y, pMaxX, pMaxY, borderCol, rounding)
        } else {
            val fillCol = ImGui.getColorU32(1f, 1f, 1f, 0.05f)
            val borderCol = ImGui.getColorU32(1f, 1f, 1f, 0.28f)
            dl.addRectFilled(pMin.x, pMin.y, pMaxX, pMaxY, fillCol, rounding)
            dl.addRect(pMin.x, pMin.y, pMaxX, pMaxY, borderCol, rounding)
            val hint = "No preview"
            val textW = ImGui.calcTextSizeX(hint)
            val textH = ImGui.getTextLineHeight()
            val tx = pMin.x + (slotSize - textW) * 0.5f
            val ty = pMin.y + (slotSize - textH) * 0.5f
            dl.addText(tx, ty, ImGui.getColorU32(1f, 1f, 1f, 0.35f), hint)
        }
    }

    private fun drawMetaControls() {
        ImGuiEx.enumCombo("Min Filter", meta::minFilter) { persistAndApply() }
        ImGuiEx.enumCombo("Mag Filter", meta::magFilter) { persistAndApply() }
        ImGuiEx.enumCombo("Wrap S", meta::wrapS) { persistAndApply() }
        ImGuiEx.enumCombo("Wrap T", meta::wrapT) { persistAndApply() }
        ImGuiEx.checkbox("Generate Mipmaps", meta::generateMipmaps) {
            AssetMetaService.saveTextureMeta(assetRelativePath, meta)
            runCatching { TextureLibrary.get("user:$assetRelativePath", reload = true) }
            TextureLibrary.applyMetaFor("user:$assetRelativePath")
        }
    }

    fun persistAndApply() {
        AssetMetaService.saveTextureMeta(assetRelativePath, meta)
        TextureLibrary.applyMetaFor("user:$assetRelativePath")
    }

}