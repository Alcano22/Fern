package org.fern.engine.imgui

import org.fern.engine.core.Engine
import org.fern.engine.imgui.glfw.GLFWImGuiWindowPlatform
import org.fern.engine.window.Window

interface ImGuiWindowPlatform {

    companion object {
        fun create(): ImGuiWindowPlatform = when (Engine.window.apiType) {
            Window.ApiType.GLFW -> GLFWImGuiWindowPlatform()
        }
    }

    fun init()

    fun newFrame()

    fun dispose()

}