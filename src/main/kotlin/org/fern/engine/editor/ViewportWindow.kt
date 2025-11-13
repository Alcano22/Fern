package org.fern.engine.editor

import imgui.ImGui
import imgui.ImVec2
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiDir
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import org.fern.engine.imgui.ImGuiEx
import org.fern.engine.imgui.ImGuiFonts
import org.fern.engine.logger.logger
import org.fern.engine.renderer.Framebuffer
import kotlin.math.max
import kotlin.math.round

class ViewportWindow(
    private val framebuffer: Framebuffer,
    private val targetAspect: Float? = null
) : EditorWindow(
    id = "viewport",
    title = "Viewport",
    closeable = true,
    hasMenuBar = true,
    hasContextMenu = false,
    flags = ImGuiWindowFlags.NoScrollbar or ImGuiWindowFlags.NoScrollWithMouse,
    category = "Scene"
) {

    companion object {
        val logger = logger()
    }

    var targetDisplay = 0
        private set

    var overlayMessage: String? = null

    private var showStats = false
    private var fps = 0
    private var fpsAccum = 0f
    private var fpsFrames = 0

    override fun update(dt: Float) {
        fpsAccum += dt
        fpsFrames++
        if (fpsAccum < 1f) return

        fps = round(fpsFrames / fpsAccum).toInt()
        fpsAccum = 0f
        fpsFrames = 0
    }

    override fun renderMenuBar() {
        ImGuiEx.disabled(targetDisplay <= 0) {
            if (ImGui.arrowButton("##disp_left", ImGuiDir.Left))
                targetDisplay--
        }

        ImGui.sameLine()
        ImGui.text("Display $targetDisplay")
        ImGui.sameLine()
        if (ImGui.arrowButton("##disp_right", ImGuiDir.Right))
            targetDisplay++

        ImGui.sameLine()

        ImGuiEx.button("Stats") {
            showStats = !showStats
        }
    }

    override fun renderContent() {
        val viewportSize = computeLargestFittingSize()
        val targetWidth = viewportSize.x.toInt().coerceAtLeast(1)
        val targetHeight = viewportSize.y.toInt().coerceAtLeast(1)

        if (framebuffer.width != targetWidth || framebuffer.height != targetHeight)
            framebuffer.resize(targetWidth, targetHeight)

        val viewportPos = computeCenteredCursorPos(viewportSize)
        ImGui.setCursorPos(viewportPos)

        if (overlayMessage == null) {
            val textureId = framebuffer.colorAttachmentId.toLong()
            ImGui.image(textureId, viewportSize, ImVec2(0f, 1f), ImVec2(1f, 0f))
        } else {
            ImGui.dummy(viewportSize)
            drawCenteredTextOverlay(viewportPos, viewportSize, overlayMessage!!)
        }

        overlayMessage?.let { drawCenteredTextOverlay(viewportPos, viewportSize, it) }

        if (showStats)
            drawStatsOverlay(viewportPos)
    }

    private fun drawStatsOverlay(pos: ImVec2) {
        val pad = 10f
        ImGui.setCursorPos(pos.x + pad, pos.y + pad)
        ImGuiEx.withStyle(
            {
                style(ImGuiStyleVar.ChildRounding, 10f)
                color(ImGuiCol.ChildBg, 0f, 0f, 0f, 0.4f)
            }
        ) {
            if (ImGui.beginChild("Stats", 250f, 150f, true)) {
                ImGui.text("Stats")
                ImGui.separator()
                ImGui.text("FPS: $fps")
                ImGui.endChild()
            }
        }
    }

    private fun drawCenteredTextOverlay(pos: ImVec2, size: ImVec2, text: String) {
        ImGuiFonts.withLarge {
            val textSize = ImGui.calcTextSize(text)
            val cx = pos.x + (size.x - textSize.x) * 0.5f
            val cy = pos.y + (size.y - textSize.y) * 0.5f
            ImGui.setCursorPos(cx, cy)
            ImGui.text(text)
        }
    }

    private fun computeLargestFittingSize(): ImVec2 {
        val avail = ImGui.getContentRegionAvail()
        val availX = max(avail.x, 1f)
        val availY = max(avail.y, 1f)

        val aspect = targetAspect?.coerceAtLeast(1e-6f) ?: (availX / max(1f, availY))
        var w = availX
        var h = w / aspect
        if (h > availY) {
            h = availY
            w = h * aspect
        }
        return ImVec2(w, h)
    }

    private fun computeCenteredCursorPos(contentSize: ImVec2): ImVec2 {
        val cursor = ImGui.getCursorPos()
        val avail = ImGui.getContentRegionAvail()
        val offX = (max(avail.x, 1f) - contentSize.x) * 0.5f
        val offY = (max(avail.y, 1f) - contentSize.y) * 0.5f
        return ImVec2(cursor.x + offX, cursor.y + offY)
    }

}