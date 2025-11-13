package org.fern.engine.imgui.glfw

import imgui.glfw.ImGuiImplGlfw
import org.fern.engine.core.Engine
import org.fern.engine.imgui.ImGuiWindowPlatform

class GLFWImGuiWindowPlatform : ImGuiWindowPlatform {

    private val implGlfw = ImGuiImplGlfw()

    override fun init() {
        val nativeWindow = Engine.window.getHandle()
        implGlfw.init(nativeWindow, true)
    }

    override fun newFrame() = implGlfw.newFrame()

    override fun dispose() = implGlfw.shutdown()

}