package org.fern.engine.imgui

import imgui.ImFontConfig
import imgui.ImGui
import imgui.flag.ImGuiConfigFlags
import org.fern.engine.layer.Layer
import org.fern.engine.resource.ResourceLoader

class ImGuiLayer : Layer("ImGuiLayer") {

    private val windowPlatform = ImGuiWindowPlatform.create()
    private val renderPlatform = ImGuiRenderPlatform.create()

    override fun onAttach() {
        ImGui.createContext()

        val io = ImGui.getIO()
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard)
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable)

        ImGui.styleColorsDark()

        ImGuiFonts.load("internal:fonts/IBMPlexSans-Medium.ttf")

        windowPlatform.init()
        renderPlatform.init()
    }

    override fun onDetach() {
        renderPlatform.dispose()
        windowPlatform.dispose()
        ImGui.destroyContext()
    }

    fun begin() {
        renderPlatform.newFrame()
        windowPlatform.newFrame()
        ImGui.newFrame()
    }

    fun end() {
        ImGui.render()
        renderPlatform.renderDrawData(ImGui.getDrawData())
    }

}