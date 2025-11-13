package org.fern.engine.renderer

import org.fern.engine.renderer.opengl.GLFramebuffer

data class FramebufferSpecs(
    var width: Int,
    var height: Int,
    var samples: Int = 1,
    var swapChainTarget: Boolean = false
)

interface Framebuffer {

    companion object {
        fun create(specs: FramebufferSpecs) = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLFramebuffer(specs)
        }

        fun unbind() = when (Renderer.apiType) {
            RenderAPI.Type.OPEN_GL -> GLFramebuffer.unbind()
        }
    }

    val specs: FramebufferSpecs

    val colorAttachmentId: Int

    val isBound: Boolean

    val width get() = specs.width
    val height get() = specs.height

    fun resize(width: Int, height: Int)

    fun bind()

    fun dispose()

    fun getId(): Int

}