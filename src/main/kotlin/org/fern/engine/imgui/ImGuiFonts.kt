package org.fern.engine.imgui

import imgui.ImFont
import imgui.ImFontConfig
import imgui.ImGui
import org.fern.engine.resource.ResourceLoader

object ImGuiFonts {

    lateinit var DEFAULT: ImFont
        private set
    lateinit var LARGE: ImFont
        private set

    fun load(
        path: String,
        defaultSize: Float = 20f,
        largeSize: Float = 40f,
    ) {
        val io = ImGui.getIO()
        val atlas = io.fonts

        atlas.clear()

        val fontCfg = ImFontConfig()
        fontCfg.fontDataOwnedByAtlas = false
        fontCfg.oversampleH = 2
        fontCfg.oversampleV = 2

        val fontData = ResourceLoader.loadBytes(path)
        DEFAULT = atlas.addFontFromMemoryTTF(fontData, defaultSize, fontCfg)
        LARGE   = atlas.addFontFromMemoryTTF(fontData, largeSize, fontCfg)

        io.fontDefault = DEFAULT
    }

    inline fun withDefault(block: () -> Unit) = ImGuiEx.withFont(DEFAULT, block)
    inline fun withLarge(block: () -> Unit) = ImGuiEx.withFont(LARGE, block)

}