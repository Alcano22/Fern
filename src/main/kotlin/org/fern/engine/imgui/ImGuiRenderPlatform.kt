package org.fern.engine.imgui

import imgui.ImDrawData
import org.fern.engine.imgui.opengl.GLImGuiRenderPlatform
import org.fern.engine.renderer.RenderAPI
import org.fern.engine.renderer.Renderer

interface ImGuiRenderPlatform {

    companion object {
        fun create() = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLImGuiRenderPlatform()
        }
    }

    fun init()

    fun newFrame()
    fun renderDrawData(drawData: ImDrawData)

    fun dispose()

}