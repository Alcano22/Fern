package org.fern.engine.imgui.opengl

import imgui.ImDrawData
import imgui.gl3.ImGuiImplGl3
import org.fern.engine.imgui.ImGuiRenderPlatform

class GLImGuiRenderPlatform : ImGuiRenderPlatform {

    private val implGl3 = ImGuiImplGl3()

    override fun init() { implGl3.init("#version 450") }

    override fun newFrame() = implGl3.newFrame()

    override fun renderDrawData(drawData: ImDrawData) =
        implGl3.renderDrawData(drawData)

    override fun dispose() = implGl3.shutdown()

}